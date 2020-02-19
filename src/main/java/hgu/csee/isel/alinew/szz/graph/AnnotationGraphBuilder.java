package hgu.csee.isel.alinew.szz.graph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import com.google.common.collect.Iterators;

import hgu.csee.isel.alinew.szz.exception.EmptyHunkTypeException;
import hgu.csee.isel.alinew.szz.model.Hunk;
import hgu.csee.isel.alinew.szz.model.Line;
import hgu.csee.isel.alinew.szz.model.LineType;
import hgu.csee.isel.alinew.szz.model.RevsWithPath;
import hgu.csee.isel.alinew.szz.util.GitUtils;
import hgu.csee.isel.alinew.szz.util.Utils;

public class AnnotationGraphBuilder {

	public AnnotationGraphModel buildAnnotationGraph(Repository repo, RevsWithPath revsWithPath, boolean debug)
			throws IOException, EmptyHunkTypeException {
		AnnotationGraphModel agm = new AnnotationGraphModel();

		HashMap<String, ArrayList<Line>> childPathWithLines = new HashMap<String, ArrayList<Line>>();
		HashMap<String, ArrayList<Line>> parentPathWithLines =  new HashMap<String, ArrayList<Line>>();
		
		int childIdx, hunkIdx, offset;
		int beginOfChild, endOfChild;
		Line childLine;
		Hunk hunk;
		String hunkType;

		// traverse all paths in the repo
		Iterator<String> paths = revsWithPath.keySet().iterator();

		Iterator<String> pathsForCalculatingSize = revsWithPath.keySet().iterator();
		int numOfPaths = Iterators.size(pathsForCalculatingSize);

		int pathCnt = 1;
		while (paths.hasNext()) {

			String path = paths.next();

			List<RevCommit> revs = revsWithPath.get(path);

			ArrayList<Line> parentLineList = new ArrayList<>();
			ArrayList<Line> childLineList = new ArrayList<>();

			int revCnt = 1;
			for (RevCommit childRev : revs) {
				// Logging
				if (!debug) {
					System.out.println("\nPaths : " + pathCnt + " / " + numOfPaths);
					System.out.println("Revs : " + revCnt + " / " + revs.size());
					System.out.println("\tPath : " + path);
					System.out.println("\tRevision : " + childRev.getName() + "\n");
				}

				// Escape from the loop when there is no parent rev anymore
				if (revs.indexOf(childRev) == revs.size() - 1)
					break;
				
				childPathWithLines = new HashMap<String, ArrayList<Line>>();
				parentPathWithLines = new HashMap<String, ArrayList<Line>>();

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
							String mergedParentContent = Utils.mergeLineList(
									parentLineList.subList(hunk.getBeginOfParent(), hunk.getEndOfParent()));
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
							throw new EmptyHunkTypeException();
						}
					}

					childIdx++;
				}

				// make HashMap<path, childLineList> and HashMap<path, parentList>
				childPathWithLines.put(path, childLineList);
				parentPathWithLines.put(path, parentLineList);

				// put subgraph into graph(i.e. AnnotationGraphModel)
				agm.put(childRev, childPathWithLines);
				agm.put(parentRev, parentPathWithLines);

				childLineList = parentLineList;
				parentLineList = new ArrayList<Line>();

				revCnt++;
			}
			pathCnt++;
		}

		return agm;
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
			Line line = new Line(path, rev.getName(), contentArr[i], i, ancestors);

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
