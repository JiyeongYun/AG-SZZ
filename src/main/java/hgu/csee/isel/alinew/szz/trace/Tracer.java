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
import hgu.csee.isel.alinew.szz.data.BICInfo;

public class Tracer {
	private boolean analysis;
	private static final int REFACTOIRNG_THRESHOLD = 10;
	private HashSet<Line> BILines = new HashSet<>();
	private List<BICInfo> bicList = new ArrayList<>();
	private static ArrayList<Line> formatChangedLineList = new ArrayList<Line>();

	public Tracer(boolean analysis) {
		this.analysis = analysis;
	}

	public List<BICInfo> collectBILines(Repository repo, List<RevCommit> BFCList, AnnotationGraphModel annotationGraph,
			RevsWithPath revsWithPath, boolean debug) throws IOException {

		// Phase 1 : Find path and line index for tracing
		for (RevCommit BFC : BFCList) {
			if (BFC.getParentCount() == 0)
				continue;

			RevCommit parentRev = BFC.getParent(0); // Get BFC pre-commit (i.e. BFC~1 commit)
			if (parentRev == null) {
				System.err.println("ERROR: Parent commit does not exist: " + BFC.name());
				break;
			}

			if (debug) {
				System.out.println("\nParent Revision : " + parentRev.getName());
				System.out.println("Child Revision (BFC) : " + BFC.getName());
			}

			List<DiffEntry> diffs = GitUtils.diff(repo, parentRev.getTree(), BFC.getTree());

			/*
			 * HEURISTIC : If the number of changed path in BFC is greater than 10, that
			 * commit is highly likely to involve refactoring codes that can be noise for
			 * collecting BIC.
			 */
			if (REFACTOIRNG_THRESHOLD <= diffs.size())
				continue;

			for (DiffEntry diff : diffs) {
				String path = diff.getNewPath();

				// Ignore non-java file and test file
				if (!path.endsWith(".java") || path.contains("test"))
					continue;

				if (debug) {
					System.out.println("\nChanged Path : " + path);
					System.out.println("Graph contains " + path + "? " + annotationGraph.containsKey(path));

					HashMap<RevCommit, ArrayList<Line>> subAG = annotationGraph.get(path);
					if (subAG != null) {
						System.out.println("Sub Graph contains " + BFC.getName() + "? " + subAG.containsKey(BFC));
					}
				}

				// get subAnnotationGraph
				HashMap<RevCommit, ArrayList<Line>> subAnnotationGraph = annotationGraph.get(path);

				// Skip when subAnnotationGraph is null, because building AG could be omitted
				// for some reasons.
				// For example, building AG is omitted when there are only one path. See
				// AnnotationGraphBuilderThread.java
				if (subAnnotationGraph == null)
					continue;

				// get list of lines of BFC
				ArrayList<Line> linesToTrace = subAnnotationGraph.get(BFC);

				// get preFixSource and fixSource
				String parentContent = Utils.removeComments(GitUtils.fetchBlob(repo, parentRev, path)).trim();
				String childContent = Utils.removeComments(GitUtils.fetchBlob(repo, BFC, path)).trim();

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
						 * Latest ------------> Oldest [][][][][][][][][][][][][][][]
						 */
						List<RevCommit> changeRevsWithPath = revsWithPath.get(path);
						RevCommit changedPreBugFixRev = changeRevsWithPath.get(changeRevsWithPath.indexOf(BFC) + 1);

						linesToTrace = annotationGraph.get(path).get(changedPreBugFixRev);

						break;

					case REPLACE:
						begin = edit.getBeginB();
						end = edit.getEndB();

						linesToTrace = annotationGraph.get(path).get(BFC);
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

					// Phase 2 : trace
					if (0 <= begin && 0 <= end) {
						for (int i = begin; i < end; i++) {
							Line line = linesToTrace.get(i);
							
							if(analysis) {
								traceWithAnalysis(line, BFC.getName());
							} else {
								trace(line);
							}
						}
					}

				}

				String fixSha1 = BFC.name() + "";
				String fixDate = Utils.getStringDateTimeFromCommitTime(BFC);

				for (Line line : BILines) {
					BICInfo bicInfo = new BICInfo(fixSha1, path, fixDate, line);
					bicList.add(bicInfo);
				}

				BILines.clear();
			}
		}

		return bicList;
	}
	public void trace(Line line) {
		for (Line ancestor : line.getAncestors()) {
			// Lines that are not white space, not format change, and within hunk are BI Lines.
			if (!Utils.isWhitespace(ancestor.getContent())) {
				if (ancestor.isFormatChange() || !ancestor.isWithinHunk()) {
					trace(ancestor);
				} else {
					BILines.add(ancestor);
				}
			}
		}
	}

	public void traceWithAnalysis(Line line, String BFC) {
		for (Line ancestor : line.getAncestors()) {
			// Lines that are not white space, not format change, and within hunk are BI Lines.
			if (!Utils.isWhitespace(ancestor.getContent())) {
				if(ancestor.isFormatChange()) {
					if (!formatChangedLineList.contains(line)) {
						System.out.println("BFC : " +  BFC);
						System.out.println("Format Change Path : " + line.getPath());
						System.out.println("Format Change Revision : " + line.getRev());
						System.out.println("Format Change Content : " + line.getContent() + "\n");

						formatChangedLineList.add(line);
					}
					
					traceWithAnalysis(ancestor, BFC);
					
				} else if(!ancestor.isWithinHunk()) {
					
					traceWithAnalysis(ancestor, BFC);					
				} else {
					BILines.add(ancestor);
				}	
			}
		}
	}
}
