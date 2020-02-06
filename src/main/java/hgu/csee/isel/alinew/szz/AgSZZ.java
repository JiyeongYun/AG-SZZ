package hgu.csee.isel.alinew.szz;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.Edit.Type;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import hgu.csee.isel.alinew.szz.exception.EmptyHunkTypeException;
import hgu.csee.isel.alinew.szz.graph.AnnotationGraphBuilder;
import hgu.csee.isel.alinew.szz.graph.AnnotationGraphModel;
import hgu.csee.isel.alinew.szz.model.Line;
import hgu.csee.isel.alinew.szz.model.LineType;
import hgu.csee.isel.alinew.szz.util.Utils;

public class AgSZZ {
	private final String GIT_DIR = "/Users/yoon/git/DataForSZZ2";
//	private final String FIX_COMMIT = "768b0df07b2722db926e99a8f917deeb5b55d628";
	private final String FIX_COMMIT = "8cc78ae9f7ac718a8ec5c6baac371f2891941cba";
	private ArrayList<Line> BICList = new ArrayList<>();
	private HashSet<Line> BICSet = new HashSet<Line>();
	
	private static Git git;
	private Repository repo;
	
	
	public static void main(String[] args) {
		new AgSZZ().run();	
	}
	
	private void run() {
		try {
			git = Git.open(new File(GIT_DIR));
			repo = git.getRepository();
			List<RevCommit> commits = Utils.getCommits(git);
			
			// Phase 1 : Build the annotation graph
			AnnotationGraphBuilder agb = new AnnotationGraphBuilder(repo, commits);
			AnnotationGraphModel agm = agb.buildAnnotationGraph();
			
			// TEST
//			Iterator<RevCommit> iter = agm.keySet().iterator();
//			
//			int revCnt = 1;
//			while(iter.hasNext()) {
//				
//				RevCommit rev = iter.next();
//				
//				System.out.println("\nRev Count : " + revCnt + "=========================\n");
//				
//				System.out.println("rev : " + rev.getName());
//				HashMap<String, ArrayList<Line>> map = agm.get(rev);
//				Iterator<String> iter2 = map.keySet().iterator();
//				
//				while(iter2.hasNext()) {
//					String path = iter2.next();
//					System.out.println("path : " + path);
//					ArrayList<Line> lines = map.get(path);
//					
//					for(Line l : lines) {
//						System.out.println("\trev : " + l.getRev());
//						System.out.println("\tpath : " + l.getPath() + "\n\n");
//						
//						System.out.println("content : " + l.getContent());
//						System.out.println("index : " + l.getIdx());
//						System.out.println("type : " + l.getLineType() + "\n") ;
//					}
//					
//				}
//				
//				revCnt++;
//			}
			
			// TODO Phase 2 : Trace and collect BIC candidates
			DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);

			for (RevCommit childRev : commits) {
				if (childRev.getName().equals(FIX_COMMIT)) { // when we found BFC on commits
					RevCommit parentRev = childRev.getParent(0); // Get BFC pre-commit (i.e. BFC~1 commit)
					if (parentRev == null) {
						System.err.println("WARNING: Parent commit does not exist: " + childRev.name());
						break;
					}

					df.setRepository(repo);
					df.setDiffAlgorithm(Utils.diffAlgorithm);
					df.setDiffComparator(Utils.diffComparator);
//					df.setDetectRenames(true);
					df.setPathFilter(PathSuffixFilter.create(".java"));

					// do diff
					List<DiffEntry> diffs = df.scan(parentRev.getTree(), childRev.getTree());
					for (DiffEntry diff : diffs) {
						String parentPath = diff.getOldPath();
						String childPath = diff.getNewPath();

						// get preFixSource and fixSource without comments
						String parentContent = Utils.fetchBlob(repo, parentRev, parentPath);
						String childContent = Utils.fetchBlob(repo, childRev, childPath);

						// get line indices that are related to BI lines.
						EditList editList = Utils.getEditListFromDiff(parentContent, childContent);
						for (Edit edit : editList) {
							int begin = -1, end = -1;
							ArrayList<Line> lines = agm.get(childRev).get(childPath);
							
							switch(edit.getType()) {
								case DELETE:
									begin = edit.getBeginA();
									end = edit.getEndA();
//									trace(lines, begin, end);
									break;
							
								case REPLACE:
									begin = edit.getBeginB();
									end = edit.getEndB();
//									trace(lines, begin, end);
									break;
								
								default:
									
									break;
							}
							
							if(0 <= begin && 0 <= end) {
								for(int i = begin; i < end; i++) {
									Line line = lines.get(i);
									trace(line);
								}
							}
							
						
//							trace(agm, childPath, childRev, lines, begin, end, 0);	
								
							// TEST
							System.out.println("Type : " + edit.getType());
							System.out.println("begin : " + begin);
							System.out.println("end : " + end);
							System.out.println("");

						}
					}
				}
			}
			
			// TEST
			
			for(Line line : BICSet) {
				System.out.println("BIC: "+line.getIdx());
				System.out.println("Path: "+line.getPath());
				System.out.println("Revision: "+line.getRev());
				System.out.println("Content: "+line.getContent() +"\n");
			}
			
			
			
			
			// TODO Phase 3 : Filter out format changes, comments, etc among BIC candidates
			
			
			
			
		} catch (IOException | GitAPIException | EmptyHunkTypeException e) {
			e.printStackTrace();
		} 
	}
	
	public void trace(Line line) {
		
//		child 리비젼에 몇번째 line인지 index 가져오기~!~! 
		
		///////////
		// trace //
		///////////
				
		// if there is no ancestor, that is BIC
		if(line.getAncestors().size() == 0) {
			BICSet.add(line);
			return;
		}
				
		for(Line ancestor : line.getAncestors()) {
			trace(ancestor);		
		}
					
		
				
			
	}
	
}
