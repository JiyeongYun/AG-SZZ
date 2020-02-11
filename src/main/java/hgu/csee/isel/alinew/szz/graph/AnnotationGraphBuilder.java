package hgu.csee.isel.alinew.szz.graph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import hgu.csee.isel.alinew.szz.exception.EmptyHunkTypeException;
import hgu.csee.isel.alinew.szz.model.Hunk;
import hgu.csee.isel.alinew.szz.model.Line;
import hgu.csee.isel.alinew.szz.model.LineType;
import hgu.csee.isel.alinew.szz.model.PathRevision;
import hgu.csee.isel.alinew.szz.model.RevsWithPath;
import hgu.csee.isel.alinew.szz.util.Utils;

public class AnnotationGraphBuilder {
	private Repository repo;
	private List<RevCommit> commits;

	public AnnotationGraphBuilder(Repository repo, List<RevCommit> commits) {
		super();
		this.repo = repo;
		this.commits = commits;
	}

	public AnnotationGraphModel buildAnnotationGraph() throws MissingObjectException, IncorrectObjectTypeException,
			CorruptObjectException, IOException, EmptyHunkTypeException {
		AnnotationGraphModel agm = new AnnotationGraphModel();

		HashMap<String, ArrayList<Line>> childPathWithLines;
		HashMap<String, ArrayList<Line>> parentPathWithLines;

		int childIdx, hunkIdx, offset;
		int beginOfChild, endOfChild;
		Line childLine;
		Hunk hunk;
		String hunkType;

		List<PathRevision> pathRevList = configurePathRevisionList(repo, commits);
		System.out.println("pathRevList size: " + pathRevList.size());
		RevsWithPath revsWithPath = collectRevsWithSpecificPath(pathRevList);

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

				String parentContent = Utils.fetchBlob(repo, parentRev, path);
				String childContent = Utils.fetchBlob(repo, childRev, path);

				// get the parent line list from content
				configureLineList(parentLineList, path, parentRev, parentContent);

				// get the child line list only when initial iteration
				if (revs.indexOf(childRev) == 0)
					configureLineList(childLineList, path, childRev, childContent);

				ArrayList<Hunk> hunkList = configureHunkList(Utils.getEditListFromDiff(parentContent, childContent));

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
//						for(Line line : childLineList) {
//							System.out.println("path: "+line.getPath());
//							System.out.println("rev: "+line.getRev());
//							System.out.println("lineType: "+line.getLineType());
//							System.out.println("curr line idx: "+line.getIdx());
//							List<Line> lineList = new ArrayList<>();
//							lineList = line.getAncestors();
//								
//							for(Line l : lineList) {
//								System.out.println("\tparent idx: "+l.getIdx());
//							}
				//
//							System.out.println("\n\n	");
//						}

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

	private List<PathRevision> configurePathRevisionList(Repository repo, List<RevCommit> commits) throws IOException {
		List<PathRevision> paths = new ArrayList<>();

		for (RevCommit commit : commits) {
			// skip the last commit
			if (commits.indexOf(commit) == commits.size() - 1) break;

			RevCommit parent = commit.getParent(0);
			if (parent == null) break;

			List<DiffEntry> diffs = Utils.diff(repo, parent.getTree(), commit.getTree());

			// get changed paths
			for (DiffEntry diff : diffs) {
				String path = diff.getNewPath();

				// Filter only java file
				if (path.endsWith(".java")) {
					paths.add(new PathRevision(path, commit));
				}
			}
		}
		
		return paths;
	}

	private RevsWithPath collectRevsWithSpecificPath(List<PathRevision> paths)
			throws MissingObjectException, IncorrectObjectTypeException, CorruptObjectException, IOException {
		RevsWithPath revsInPath = new RevsWithPath();

		for (PathRevision pr : paths) {
			if (revsInPath.containsKey(pr.getPath())) {
				List<RevCommit> lst = revsInPath.get(pr.getPath());
				lst.add(pr.getCommit());
				revsInPath.replace(pr.getPath(), lst);
			} else {
				List<RevCommit> lst = new ArrayList<>();
				lst.add(pr.getCommit());
				revsInPath.put(pr.getPath(), lst);

			}

		}

		// TEST
//		Iterator<String> pathList = revsInPath.keySet().iterator();
//
//		while (pathList.hasNext()) {
//			String path = pathList.next();
//			System.out.println("path: " + path);
//			List<RevCommit> list = revsInPath.get(path);
//			
//			for (RevCommit rev : list) {
//				System.out.println("	rev: " + rev.getName());
//			}
//		}		

		return revsInPath;
	}

	private void configureLineList(ArrayList<Line> lst, String path, RevCommit rev, String content) {
		String[] lineContentArr = content.split("\n");

		for (int i = 0; i < lineContentArr.length; i++) {
			String lineContent = lineContentArr[i];

			// make new Line
			List<Line> ancestors = new ArrayList<>();
			Line line = new Line(path, rev.getName(), lineContent, i, ancestors);

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
