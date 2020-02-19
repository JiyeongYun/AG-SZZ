package hgu.csee.isel.alinew.szz.trace;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import hgu.csee.isel.alinew.szz.graph.AnnotationGraphModel;
import hgu.csee.isel.alinew.szz.model.Line;
import hgu.csee.isel.alinew.szz.model.RevsWithPath;
import hgu.csee.isel.alinew.szz.util.GitUtils;
import hgu.csee.isel.alinew.szz.util.Utils;

public class Tracer {
	private static final int REFACTOIRNG_THRESHOLD = 10;
	private HashSet<Line> BILines = new HashSet<>();

	public Tracer() {

	}

	public List<Line> collectBILines(Repository repo, List<RevCommit> revs, AnnotationGraphModel agm,
			RevsWithPath revsWithPath, List<String> BFCList, boolean debug) throws IOException {
		// Phase 1 : traverse all commits and find BFC
		for (String bfc : BFCList) {
			for (RevCommit childRev : revs) {
				if (childRev.getName().equals(bfc)) {
					// Phase 2 : Find path and line index for tracing
					RevCommit parentRev = childRev.getParent(0); // Get BFC pre-commit (i.e. BFC~1 commit)
					if (parentRev == null) {
						System.err.println("ERROR: Parent commit does not exist: " + childRev.name());
						break;
					}

					if (debug) {
						System.out.println("\nParent Revision : " + parentRev.getName());
						System.out.println("Child Revision (BFC) : " + childRev.getName());
					}

					List<DiffEntry> diffs = GitUtils.diff(repo, parentRev.getTree(), childRev.getTree());

					/*
					 * HEURISTIC : If the number of changed path in BFC is greater than 10, that
					 * commit is highly likely to involve refactoring codes that can be noise for
					 * collecting BIC.
					 */
					if (REFACTOIRNG_THRESHOLD <= diffs.size())
						continue;

					for (DiffEntry diff : diffs) {
						String path = diff.getNewPath();

						if (debug) {
							System.out.println("\nChanged Path : " + path);

							HashMap<String, ArrayList<Line>> subAnnotationGraphModel = agm.get(childRev);
							System.out.println("Sub Graph contains " + path + "? " + subAnnotationGraphModel.containsKey(path));
						}

						// get list of lines of BFC
						ArrayList<Line> linesToTrace = agm.get(childRev).get(path);

						// get preFixSource and fixSource
						String parentContent = Utils.removeComments(GitUtils.fetchBlob(repo, parentRev, path)).trim();
						String childContent = Utils.removeComments(GitUtils.fetchBlob(repo, childRev, path)).trim();

						// get line indices that fix bug
						EditList editList = GitUtils.getEditListFromDiff(parentContent, childContent);
						for (Edit edit : editList) {
							int begin = -1;
							int end = -1;

							if (debug) {
								System.out.println("\nHunk Info");
								System.out.println("\tType : " + edit.getType());
								System.out.println("\tbA : " + edit.getBeginA());
								System.out.println("\teA : " + edit.getEndA());
								System.out.println("\tbB : " + edit.getBeginB());
								System.out.println("\teB : " + edit.getEndB());
							}

							switch (edit.getType()) {
							case DELETE:
								begin = edit.getBeginA();
								end = edit.getEndA();

								/*
								 * Get a revision just before BFC among changed revisions with path
								 * 
								 * [REMARK] This list is sorted in chronological order.
								 * 
								 * Latest ------------> Oldest 
								 * [][][][][][][][][][][][][][][]
								 */
								List<RevCommit> changeRevsWithPath = revsWithPath.get(path);
								RevCommit changedPreBugFixRev = changeRevsWithPath
										.get(changeRevsWithPath.indexOf(childRev) + 1);

								linesToTrace = agm.get(changedPreBugFixRev).get(path);

								break;

							case REPLACE:
								begin = edit.getBeginB();
								end = edit.getEndB();
								break;

							default:
								break;
							}

							if (debug) {
								System.out.println("\nTraced Line Info : " + begin);
								System.out.println("Begin : " + begin);
								System.out.println("End : " + end);

								System.out.println("\nSize of lines to trace : " + linesToTrace.size());

								for (Line line : linesToTrace) {

									int lindIdx = line.getIdx();
									if (lindIdx >= begin && lindIdx < end) {
										System.out.println("\nLine Idx : " + line.getIdx());
										System.out.println("Content : " + line.getContent());
									}
								}
							}

							// Phase 3 : trace
							if (0 <= begin && 0 <= end) {
								for (int i = begin; i < end; i++) {
									Line line = linesToTrace.get(i);
									trace(line);
								}
							}
						}
					}
				}
			}
		}

		List<Line> BILinesWithoutDuplicates = new ArrayList<>(BILines);

		return BILinesWithoutDuplicates;
	}

	public void trace(Line line) {
		for (Line ancestor : line.getAncestors()) {
			// neither whitespace nor format change is the Bug Introducing Lines
			if (!Utils.isWhitespace(ancestor.getContent())) {
				if (ancestor.isFormatChange()) {
					trace(ancestor);
				} else {
					BILines.add(ancestor);
				}
			}
		}
	}
}
