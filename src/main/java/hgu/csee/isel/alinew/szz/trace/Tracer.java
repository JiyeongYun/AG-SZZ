package hgu.csee.isel.alinew.szz.trace;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import hgu.csee.isel.alinew.szz.graph.AnnotationGraphModel;
import hgu.csee.isel.alinew.szz.model.Line;
import hgu.csee.isel.alinew.szz.util.Utils;

public class Tracer {
	private static final int REFACTOIRNG_THRESHOLD = 10;
	private HashSet<Line> BILines = new HashSet<>();

	public Tracer() {
		// TODO Auto-generated constructor stub
	}
	
	public List<Line> collectBILines(Repository repo, List<RevCommit> revs, AnnotationGraphModel agm, List<String> BFCList) throws IOException{
		// Phase 1 : traverse all commits and find BFC
		for(String bfc : BFCList) {
			for(RevCommit childRev : revs) {
				if(childRev.getName().equals(bfc)) {
		// Phase 2 : Find path and line index for tracing
					RevCommit parentRev = childRev.getParent(0); // Get BFC pre-commit (i.e. BFC~1 commit)
					if (parentRev == null) {
						System.err.println("ERROR: Parent commit does not exist: " + childRev.name());
						break;
					}
					
					List<DiffEntry> diffs = Utils.diff(repo, parentRev.getTree(), childRev.getTree());
					
					/* 
					 * HEURISTIC : If the number of changed path in BFC is greater than 10, 
					 * that commit is highly likely to involve refactoring codes that can be noise for collecting BIC.
					*/
					if(REFACTOIRNG_THRESHOLD <= diffs.size()) continue;
					
					for (DiffEntry diff : diffs) {
						String path = diff.getNewPath();
						
						// get list of lines of BFC
						ArrayList<Line> lines = agm.get(childRev).get(path);
						ArrayList<Line> linesOfParent = agm.get(parentRev).get(path);
						
						// get preFixSource and fixSource
						String parentContent = Utils.fetchBlob(repo, parentRev, path);
						String childContent = Utils.fetchBlob(repo, childRev, path);

						// get line indices that fix bug
						EditList editList = Utils.getEditListFromDiff(parentContent, childContent);
						for (Edit edit : editList) {
							int begin = -1;
							int end = -1;
							boolean isFormatChange = false;
							
							switch(edit.getType()) {
								case DELETE:
									begin = edit.getBeginA();
									end = edit.getEndA();
									break;
							
								case REPLACE:
									begin = edit.getBeginB();
									end = edit.getEndB();
									
									//get sublist of lines of parent
									int begingOfParent = edit.getBeginA();
									int endOfParent = edit.getEndA();
									
									//add all parent lines + remove white spaces
									String mergeParentContent = arrayToString(linesOfParent.subList(begingOfParent, endOfParent));

									//add all child lines + remove white spaces
									String mergeChildContent = arrayToString(lines.subList(begin, end));

									//check whether they are same
									if(mergeParentContent.equals(mergeChildContent)) isFormatChange = true;
									break;
								
								default:
									break;
							}
		//Phase 3 : trace
							
							if(isFormatChange || (0 <= begin && 0 <= end)) {
								for(int i = begin; i < end; i++) {
									Line line = lines.get(i);
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
	
	public String arrayToString(List<Line> list) {
		String mergeContent = "";
		for(Line line : list) {
			mergeContent += line.getContent();
		}
		
		return mergeContent.replaceAll("\\s", "");	
	}
	
	public void trace(Line line) {
		
		if(!Utils.isComment(line.getContent()) && !Utils.isWhitespace(line.getContent())) {
			
			// if there is no ancestor, that is BIC
			if(line.getAncestors().size() == 0) {
				BILines.add(line);
				return;
			}
		
			for(Line ancestor : line.getAncestors()) {	
				trace(ancestor);		
			}
			
		}
		
	}
}
