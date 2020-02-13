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

import hgu.csee.isel.alinew.szz.exception.EmptyHunkTypeException;
import hgu.csee.isel.alinew.szz.model.Hunk;
import hgu.csee.isel.alinew.szz.model.Line;
import hgu.csee.isel.alinew.szz.model.LineType;
import hgu.csee.isel.alinew.szz.model.RevsWithPath;
import hgu.csee.isel.alinew.szz.util.GitUtils;
import hgu.csee.isel.alinew.szz.util.Utils;

public class AnnotationGraphBuilder {

	public AnnotationGraphModel buildAnnotationGraph(Repository repo, RevsWithPath revsWithPath)
			throws IOException, EmptyHunkTypeException {
		AnnotationGraphModel agm = new AnnotationGraphModel();

		HashMap<String, ArrayList<Line>> childPathWithLines;
		HashMap<String, ArrayList<Line>> parentPathWithLines;

		int childIdx, hunkIdx, offset;
		int beginOfChild, endOfChild;
		Line childLine;
		Hunk hunk;
		String hunkType;

		// traverse all paths in the repo
		Iterator<String> paths = revsWithPath.keySet().iterator();

		while (paths.hasNext()) {
			String path = paths.next();

			List<RevCommit> revs = revsWithPath.get(path);

			ArrayList<Line> parentLineList = new ArrayList<>();
			ArrayList<Line> childLineList = new ArrayList<>();

			for (RevCommit childRev : revs) {
				// Escape from the loop when there is no parent rev anymore
				if (revs.indexOf(childRev) == revs.size() - 1)
					break;

				childPathWithLines = new HashMap<String, ArrayList<Line>>();
				parentPathWithLines = new HashMap<String, ArrayList<Line>>();

				RevCommit parentRev = revs.get(revs.indexOf(childRev) + 1);

				String parentContent = Utils.removeComments(GitUtils.fetchBlob(repo, parentRev, path));
				String childContent = Utils.removeComments(GitUtils.fetchBlob(repo, childRev, path));

				// TEST
				System.out.println("\n\npath : " + path);
				System.out.println("\tparent rev : " + parentRev.getName());
//				System.out.println("parent content : \n" + parentContent + "\n");
				System.out.println("\tchild rev : " + childRev.getName());
//				System.out.println("child content : \n" + childContent + "\n");

				// get the parent line list from content
				configureLineList(parentLineList, path, parentRev, parentContent);

				// get the child line list only when initial iteration
				if (revs.indexOf(childRev) == 0)
					configureLineList(childLineList, path, childRev, childContent);

				// TEST
//				System.out.println("Size of PLL : " + parentLineList.size());
//				System.out.println("Size of CLL : " + childLineList.size());

				ArrayList<Hunk> hunkList = configureHunkList(GitUtils.getEditListFromDiff(parentContent, childContent));

				// map child line with its ancestor(s)
				childIdx = 0;
				hunkIdx = 0;
				offset = 0;

				while (childIdx < childLineList.size()) {

					childLine = childLineList.get(childIdx);

					// Case 1 - when there is no hunk anymore
					if (hunkList.size() <= hunkIdx) {

						childLine.setLineType(LineType.CONTEXT);
						mapChildLineWithAncestor(childIdx, offset, parentLineList, childLine);

						childIdx++;
						continue;
					}

					hunk = hunkList.get(hunkIdx);
					beginOfChild = hunk.getBeginOfChild();
					endOfChild = hunk.getEndOfChild();
					hunkType = hunk.getHunkType();

					// TEST
//					System.out.println("\nBegin of parent : " + hunk.getBeginOfParent());
//					System.out.println("End of parent : " + hunk.getEndOfParent());
//					System.out.println("Begin of child : " + hunk.getBeginOfChild());
//					System.out.println("End of child : " + hunk.getEndOfChild() + "\n");

					// Case 2 - child index is out of hunk range
					if (childIdx < beginOfChild) {

						childLine.setLineType(LineType.CONTEXT);
						mapChildLineWithAncestor(childIdx, offset, parentLineList, childLine);

					}
					// Case 3 - child index is in hunk range
					else {
						switch (hunkType) {
						case "INSERT":
							// When childIdx is the last index in hunk, increment hunk index
							if (childIdx == endOfChild - 1)
								hunkIdx++;

							childLine.setLineType(LineType.INSERT);

							offset--;

							break;

						case "REPLACE":
							// When childIdx is the last index in hunk, update offset and increment hunk
							// index
							if (childIdx == endOfChild - 1) {
								offset += hunk.getRangeOfParent() - hunk.getRangeOfChild();
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
							offset += hunk.getRangeOfParent();

							childLine.setLineType(LineType.CONTEXT);
							mapChildLineWithAncestor(childIdx, offset, parentLineList, childLine);

							hunkIdx++;

							break;

						default:
							throw new EmptyHunkTypeException();
						}
					}

					childIdx++;
				}

				// TEST
//				for (Line line : childLineList) {
//					System.out.println("path: " + line.getPath());
//					System.out.println("rev: " + line.getRev());
//					System.out.println("content : " + line.getContent());
//					System.out.println("curr line idx: " + line.getIdx());
//					System.out.println("lineType: " + line.getLineType());
//
//					List<Line> ancestors = line.getAncestors();
//
//					for (Line ancestor : ancestors) {
//						System.out.println("\tparent rev: " + ancestor.getRev());
//						System.out.println("\tparent content : " + ancestor.getContent());
//						System.out.println("\tparent idx: " + ancestor.getIdx());
//					}
//
//					System.out.println("\n\n");
//				}

				// make HashMap<path, childLineList> and HashMap<path, parentList>
				childPathWithLines.put(path, childLineList);
				parentPathWithLines.put(path, parentLineList);

				// put subgraph into graph(i.e. AnnotationGraphModel)
				agm.put(childRev, childPathWithLines);
				agm.put(parentRev, parentPathWithLines);

				childLineList = parentLineList;
				parentLineList = new ArrayList<Line>();

			}
		}

		return agm;
	}

	private void configureLineList(ArrayList<Line> lst, String path, RevCommit rev, String content) {
		/*
		 * [REMARK]
		 * 
		 * (Size of lineContentArr) >= (Size of splittedContentArr)
		 * 
		 * ============= example ==============
		 *  public class HelloWorld{
		 * 		System.out.println("Hello");
		 * 
		 * }
		 * 
		 * 
		 * ===================================
		 * 
		 * (Size of lineContentArr) = 6
		 * (Size of splittedContentArr) = 4
		 * 
		 * Check the src/test/java/splitTest.java
		 */
		
		String[] lineContentArr = new String[Utils.getLineNum(content)];

		// initial setting
		for (int i = 0; i < lineContentArr.length; i++) {
			lineContentArr[i] = "";
		}

		String[] splittedContentArr = content.split("\r\n|\r|\n");

		for (int i = 0; i < splittedContentArr.length; i++) {
			lineContentArr[i] = splittedContentArr[i];
		}

		for (int i = 0; i < lineContentArr.length; i++) {
			// make new Line
			List<Line> ancestors = new ArrayList<>();
			Line line = new Line(path, rev.getName(), lineContentArr[i], i, ancestors);

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
