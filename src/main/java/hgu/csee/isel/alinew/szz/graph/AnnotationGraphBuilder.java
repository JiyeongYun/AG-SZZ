package hgu.csee.isel.alinew.szz.graph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.errors.LargeObjectException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import com.google.common.collect.Iterators;

import hgu.csee.isel.alinew.szz.AGSZZ;
import hgu.csee.isel.alinew.szz.exception.EmptyHunkTypeException;
import hgu.csee.isel.alinew.szz.model.Hunk;
import hgu.csee.isel.alinew.szz.model.Line;
import hgu.csee.isel.alinew.szz.model.LineType;
import hgu.csee.isel.alinew.szz.model.RevsWithPath;
import hgu.csee.isel.alinew.szz.util.GitUtils;
import hgu.csee.isel.alinew.szz.util.Utils;

public class AnnotationGraphBuilder extends Thread {

	private final int THREADS = 3;

	Repository repo;
	RevsWithPath revsWithPath;
	List<String> list = new ArrayList<>();
	boolean debug;
	Iterator<String> paths;

	public AnnotationGraphBuilder() {

	}

	public AnnotationGraphBuilder(Repository repo, RevsWithPath revsWithPath, List<String> list, boolean debug) {
		this.repo = repo;
		this.revsWithPath = revsWithPath;
		for (int i = 0; i < list.size(); i++) {
			this.list.add(list.get(i));
		}
//		this.list = list;
		this.debug = debug;
	}

	public void buildAnnotationGraph(Repository repo, RevsWithPath revsWithPath, boolean debug) {

		List<String> keyList = new ArrayList(revsWithPath.keySet());
		System.out.println("list의 사이즈: " + keyList.size());

		int arr_size;
		if (keyList.size() < THREADS) {
			arr_size = keyList.size();
		} else {
			arr_size = keyList.size() / THREADS;
		}

		AnnotationGraphBuilder agb1 = new AnnotationGraphBuilder(repo, revsWithPath, keyList.subList(0, arr_size), debug);
		AnnotationGraphBuilder agb2 = new AnnotationGraphBuilder(repo, revsWithPath, keyList.subList(arr_size, 2 * arr_size), debug);
		AnnotationGraphBuilder agb3 = new AnnotationGraphBuilder(repo, revsWithPath, keyList.subList(2 * arr_size, keyList.size()), debug);

		agb1.start();
		agb2.start();
		agb3.start();

		try {
			agb1.join();
			agb2.join();
			agb3.join();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// TEST
//		Iterator<String> paths = AGSZZ.agm.keySet().iterator();
//
//		while (paths.hasNext()) {
//			String path = paths.next();
//			System.out.println("\tvalue: " + path);
//		}
//
//		System.out.println("\tannotationGraph size: " + AGSZZ.agm.size());
	}

	@Override
	public void run() {
		for (int i = 0; i < list.size(); i++) {
			String path = list.get(i);

			try {
				createAnnotationGraph(path);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (EmptyHunkTypeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	private void createAnnotationGraph(String path) throws IOException, EmptyHunkTypeException {
		int childIdx, hunkIdx, offset;
		int beginOfChild, endOfChild;
		Line childLine;
		Hunk hunk;
		String hunkType;

		List<RevCommit> revs = revsWithPath.get(path);

		// Generate subAnnotationGraph
		HashMap<RevCommit, ArrayList<Line>> subAnnotationGraph = new HashMap<RevCommit, ArrayList<Line>>();

		ArrayList<Line> parentLineList = new ArrayList<>();
		ArrayList<Line> childLineList = new ArrayList<>();

		// Logging
		System.out.println("\tPresent Thread Name is " + Thread.currentThread().getName() );
		System.out.println("\tBuilding Annotation Graph of " + path);

		for (RevCommit childRev : revs) {
			// Escape from the loop when there is no parent rev anymore
			if (revs.indexOf(childRev) == revs.size() - 1)
				break;

			RevCommit parentRev = revs.get(revs.indexOf(childRev) + 1);

			String parentContent = Utils.removeComments(GitUtils.fetchBlob(repo, parentRev, path)).trim();
			String childContent = Utils.removeComments(GitUtils.fetchBlob(repo, childRev, path)).trim();

			if (debug) {
				System.out.println("path : " + path);
				System.out.println("\tparent rev : " + parentRev.getName());
				System.out.println("\tchild rev : " + childRev.getName());
			}

			// get the parent line list from content
			configureLineList(parentLineList, path, parentRev, parentContent);

			// get the child line list only when initial iteration
			if (revs.indexOf(childRev) == 0)
				configureLineList(childLineList, path, childRev, childContent);

			if (debug) {
				System.out.println("\nParent");
				for (int i = 0; i < parentLineList.size(); i++) {
					System.out.println(i + "th idx : " + parentLineList.get(i).getContent());
				}
				System.out.println("\nChild");
				for (int i = 0; i < childLineList.size(); i++) {
					System.out.println(i + "th idx : " + childLineList.get(i).getContent());
				}
			}

			ArrayList<Hunk> hunkList = configureHunkList(GitUtils.getEditListFromDiff(parentContent, childContent));

			// map child line with its ancestor(s)
			childIdx = 0;
			hunkIdx = 0;
			offset = 0;

			while (childIdx < childLineList.size()) {

				childLine = childLineList.get(childIdx);

				if (debug) {
					System.out.println("\nHunk Rate : " + (hunkIdx + 1) + " / " + hunkList.size());
					System.out.println("Child Index Rate : " + childIdx + " / " + (childLineList.size() - 1));
					System.out.println("Offset : " + offset);
				}

				// Case 1 - when there is no hunk anymore
				if (hunkList.size() <= hunkIdx) {
					if (debug) {
						System.out.println("Connected parent index : " + (childIdx + offset) + " / "
								+ (parentLineList.size() - 1));
						System.out.println("No Hunk anymore\n");
					}
					childLine.setLineType(LineType.CONTEXT);

					mapChildLineWithAncestor(childIdx, offset, parentLineList, childLine);

					childIdx++;
					continue;
				}

				hunk = hunkList.get(hunkIdx);
				beginOfChild = hunk.getBeginOfChild();
				endOfChild = hunk.getEndOfChild();
				hunkType = hunk.getHunkType();

				if (debug) {
					System.out.println("Hunk Type : " + hunk.getHunkType());
					System.out.println("bA : " + hunk.getBeginOfParent());
					System.out.println("eA : " + hunk.getEndOfParent());
					System.out.println("bB : " + hunk.getBeginOfChild());
					System.out.println("eB : " + hunk.getEndOfChild());
				}

				// Case 2 - child index is out of hunk range
				if (childIdx < beginOfChild) {
					if (debug) {
						System.out.println("Connected parent index : " + (childIdx + offset) + " / "
								+ (parentLineList.size() - 1));
						System.out.println("Out of Hunk range\n");

					}
					childLine.setLineType(LineType.CONTEXT);
					mapChildLineWithAncestor(childIdx, offset, parentLineList, childLine);

				}
				// Case 3 - child index is in hunk range
				else {
					switch (hunkType) {
					case "INSERT":
						if (debug) {
							System.out.println("INSERT\n");
						}

						// When childIdx is the last index in hunk, increment hunk index
						if (childIdx == endOfChild - 1)
							hunkIdx++;

						childLine.setLineType(LineType.INSERT);

						offset--;

						break;

					case "REPLACE":
						if (debug) {
							System.out.println("REPLACE\n");
						}

						// When childIdx is the last index in hunk, update offset and increment hunk
						// index
						if (childIdx == endOfChild - 1) {
							offset += (hunk.getRangeOfParent() - hunk.getRangeOfChild());
							hunkIdx++;
						}

						// check whether format change happens
						String mergedParentContent = Utils
								.mergeLineList(parentLineList.subList(hunk.getBeginOfParent(), hunk.getEndOfParent()));
						String mergedChildContent = Utils
								.mergeLineList(childLineList.subList(hunk.getBeginOfChild(), hunk.getEndOfChild()));

						if (mergedParentContent.equals(mergedChildContent))
							childLine.setFormatChange(true);

						childLine.setLineType(LineType.REPLACE);
						mapChildLineWithAncestors(hunk, parentLineList, childLine);

						break;

					case "DELETE":
						// If the last child line is in DELETE, it maps with nothing
						if (childIdx == childLineList.size() - 1)
							break;

						// If the begin of child belongs to both DELETE and INSERT
						if (belongsToBothDELETEAndINSERT(hunkList, hunkIdx, beginOfChild)) {
							offset += hunk.getRangeOfParent() - 1;

							childLine.setLineType(LineType.INSERT);
							hunkIdx++;

							if (debug) {
								System.out.println("INSERT\n");
							}

							break;
						}

						offset += hunk.getRangeOfParent();

						childLine.setLineType(LineType.CONTEXT);
						mapChildLineWithAncestor(childIdx, offset, parentLineList, childLine);

						hunkIdx++;

						if (debug) {
							System.out.println("Connected parent index : " + (childIdx + offset) + " / "
									+ (parentLineList.size() - 1));
							System.out.println("DELETE\n");
						}

						break;

					default:
						System.out.println("EmptyHunkTypeException occur");
						throw new EmptyHunkTypeException();
					}
				}

				childIdx++;
			}

			// put lists of line corresponding to commit into subAG
			subAnnotationGraph.put(parentRev, parentLineList);
			subAnnotationGraph.put(childRev, childLineList);

			childLineList = parentLineList;
			parentLineList = new ArrayList<Line>();
		}
		// put subAG corresponding to path into AG
		AGSZZ.agm.put(path, subAnnotationGraph);

	}

	private boolean belongsToBothDELETEAndINSERT(ArrayList<Hunk> hunkList, int currHunkIdx, int currBeginOfChild) {
		int nextHunkIdx = currHunkIdx + 1;

		if (nextHunkIdx < hunkList.size()) {
			String nextHunkType = hunkList.get(nextHunkIdx).getHunkType();
			int nextBeginOfChild = hunkList.get(nextHunkIdx).getBeginOfChild();

			if (nextHunkType.equals("INSERT") && currBeginOfChild == nextBeginOfChild) {
				return true;
			}
		}

		return false;
	}

	private void configureLineList(ArrayList<Line> lst, String path, RevCommit rev, String content) {
		String[] contentArr = content.split("\r\n|\r|\n");

		for (int i = 0; i < contentArr.length; i++) {
			// make new Line
			List<Line> ancestors = new ArrayList<>();
			String committer = rev.getCommitterIdent().getName();
			String author = rev.getAuthorIdent().getName();
			String StringDateTime = Utils.getStringDateTimeFromCommitTime(rev.getCommitTime());

			Line line = new Line(path, rev.getName(), contentArr[i], i, LineType.CONTEXT, ancestors, false, committer,
					author, StringDateTime);

			lst.add(line);
		}
	}

	private ArrayList<Hunk> configureHunkList(EditList editList) {
		ArrayList<Hunk> hunkList = new ArrayList<>();

		for (Edit edit : editList) {
			Hunk hunk = new Hunk(edit.getType().toString(), edit.getBeginA(), edit.getEndA(), edit.getBeginB(),
					edit.getEndB());

			hunkList.add(hunk);
		}

		return hunkList;
	}

	private void mapChildLineWithAncestor(int childIdx, int offset, List<Line> parentLineList, Line childLine) {
		Line ancestor;
		List<Line> ancestorsOfChild;

		ancestor = parentLineList.get(childIdx + offset);
		ancestorsOfChild = childLine.getAncestors();
		ancestorsOfChild.add(ancestor);
		childLine.setAncestors(ancestorsOfChild);
	}

	private void mapChildLineWithAncestors(Hunk hunk, List<Line> parentLineList, Line childLine) {

		List<Line> ancestorsOfChild = parentLineList.subList(hunk.getBeginOfParent(), hunk.getEndOfParent());
		childLine.setAncestors(ancestorsOfChild);

	}

}
