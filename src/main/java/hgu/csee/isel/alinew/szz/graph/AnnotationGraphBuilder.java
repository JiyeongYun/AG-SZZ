package hgu.csee.isel.alinew.szz.graph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;

import hgu.csee.isel.alinew.szz.model.Hunk;
import hgu.csee.isel.alinew.szz.model.Line;
import hgu.csee.isel.alinew.szz.model.LineType;
import hgu.csee.isel.alinew.szz.model.PathRevision;
import hgu.csee.isel.alinew.szz.model.RevsWithPath;
import hgu.csee.isel.alinew.szz.util.Utils;

public class AnnotationGraphBuilder {
	private Repository repo;
	private List<RevCommit> commits;
//	private PathRevision childPathRev;
//	private PathRevision parentPathRev;

	public AnnotationGraphBuilder(Repository repo, List<RevCommit> commits) {
		super();
		this.repo = repo;
		this.commits = commits;
	}

	public Repository getRepo() {
		return repo;
	}

	public void setRepo(Repository repo) {
		this.repo = repo;
	}
	
	public AnnotationGraphModel buildAnnotationGraph() throws MissingObjectException, IncorrectObjectTypeException, CorruptObjectException, IOException {
		AnnotationGraphModel agm = new AnnotationGraphModel();
		PathRevision childPathRev = new PathRevision();
		PathRevision parentPathRev = new PathRevision();
		
		// configure the list of path and revision 
		List<PathRevision> pathRevList = configurePathRevisionList(repo, commits);
		
		// collect all revisions that has specific path
		RevsWithPath revsWithPath = collectRevsWithSpecificPath(pathRevList);
		
		// traverse all paths in the repo
		Iterator<String> paths = revsWithPath.keySet().iterator();
		
		while(paths.hasNext()){
			String path = paths.next();
			
			List<RevCommit> revs = revsWithPath.get(path);
			
			LinkedList<Line> parentLineList = new LinkedList<>();
			LinkedList<Line> childLineList = new LinkedList<>();
			
			//traverse all revs that has path
			for(RevCommit childRev : revs) {
				// Escape from the loop when there is no parent rev anymore
				if(revs.indexOf(childRev) == revs.size()-1) break;
			
				RevCommit parentRev = revs.get(revs.indexOf(childRev)+1);

				// get parentFileSource and childFileSource
				String parentContent = Utils.fetchBlob(repo, parentRev, path);
				String childContent = Utils.fetchBlob(repo, childRev, path);
				
				// get the parent line list from content
				configureLineList(parentLineList, path, parentRev, parentContent);
				// get the child line list only when initial iteration
				if(revs.indexOf(childRev) == 0) {
					configureLineList(childLineList, path, parentRev, parentContent);
				}
				
				EditList editList = Utils.getEditListFromDiff(parentContent, childContent);
					
				// configure the list of hunk from edit list
				ArrayList<Hunk> hunkList = configureHunkList(editList);

				/*
				 * Start code
				 *  
				 */
				int childIdx = 0;
				int hunkIdx = 0;
				int parentIdx = 0;
				int offset = 0;
					
				// search each line
				while(childIdx < childLineList.size()) {
						
					Line childLine = childLineList.get(childIdx);
						
					// when there is no hunk anymore
					if( hunkList.size() <= hunkIdx) {
						//Line ancestor = parentLineList.get(parentIdx);
						Line ancestor = parentLineList.get(childIdx + offset);
							
						if(childLine.getAncestors().size() == 0) {
							childLine.setAncestors(new ArrayList<Line>());
						}
							
						List<Line> ancestors = childLine.getAncestors();
						ancestors.add(ancestor);
						childLine.setAncestors(ancestors);
						childIdx++;
						
						continue;
					}
						
						Hunk hunk = hunkList.get(hunkIdx);
						int beginOfChild = hunk.getBeginOfChild();
						int endOfChild = hunk.getEndOfChild();
						
						if(childIdx <hunk.getBeginOfChild()) {
							// 해당 hunk의 beginB보다 작을 때까지
							// context
							System.out.println("여기는 context입니다.");
							Line ancestor = parentLineList.get(childIdx + offset);
							List<Line> ancestors = childLine.getAncestors();
							ancestors.add(ancestor);
							childLine.setAncestors(ancestors);
							
						} else if(beginOfChild <= childIdx && childIdx < endOfChild) {
							//해당 hunk범위
							// insert, delete, replace
							String hunkType = hunk.getDiffType();
							
							switch(hunkType) {
								case "INSERT" :
									System.out.println("여기는 insert입니다.");
									childLine.setLineType(LineType.INSERT);
									offset--;
									break;
								case "REPLACE" :
									System.out.println("여기는 replace입니다.");
									childLine.setLineType(LineType.REPLACE);
									
									List<Line> ancestors = parentLineList.subList(hunk.getBeginOfParent(), hunk.getEndOfParent());

									childLine.setAncestors(ancestors);
									
									//offset이 중복으로 더해지는 걸 방지  
									if(childIdx == hunk.getEndOfChild() - 1) {
										offset += hunk.getRangeOfParent() - hunk.getRangeOfChild();
									}
									break;
								default : 
									System.err.println("ERROR");
							
							}
							
							if(childIdx == endOfChild-1) {
								hunkIdx++;
							}
							
							
						} else if(beginOfChild == endOfChild && hunk.getDiffType().equalsIgnoreCase("delete")) {
//							if( childLineList.size() <= begin) {
//								break;
//							}
							
							System.out.println("여기는 delete입니다.");
							offset += hunk.getRangeOfParent();
							
							Line ancestor = parentLineList.get(childIdx + offset);
							List<Line> ancestors = childLine.getAncestors();
							ancestors.add(ancestor);
							childLine.setAncestors(ancestors);
							
							hunkIdx++;
						}
						
						childIdx++;
					}// while done
					
					/**
					 * 
					 * End
					 * 
					 * 
					 */
					
					//TEST
					for(Line line : childLineList) {
						System.out.println("path: "+line.getPath());
						System.out.println("rev: "+line.getRev());
						System.out.println("lineType: "+line.getLineType());
						System.out.println("현재 line idx: "+line.getIdx());
						List<Line> lineList = new ArrayList<>();
						lineList = line.getAncestors();
						
						for(Line l : lineList) {
							System.out.println("parent idx: "+l.getIdx());
						}
						
						System.out.println("\n\n	");
					}
				
				
				// set childPathRev and parentPathRev
				childPathRev.setCommit(childRev);
				childPathRev.setPath(path);
				parentPathRev.setCommit(parentRev);
				parentPathRev.setPath(path);
				
				// put subgraph into graph(i.e. AnnotationGraphModel)
				agm.put(childPathRev, childLineList);
				agm.put(parentPathRev, parentLineList);
				
				// update child to parent and generate new parent
				childPathRev = parentPathRev;
				parentPathRev = new PathRevision();
				childLineList = parentLineList;
				parentLineList = new LinkedList<Line>();
			}
		}
		
		return agm;
	}
	
	private List<PathRevision> configurePathRevisionList(Repository repo, List<RevCommit> commits) throws MissingObjectException, IncorrectObjectTypeException, CorruptObjectException, IOException {
		List<PathRevision> paths = new ArrayList<>();
		
		for(RevCommit commit : commits) {
			RevTree tree = commit.getTree();
			
			TreeWalk treeWalk = new TreeWalk(repo);
			treeWalk.addTree(tree);
			treeWalk.setRecursive(true);
			treeWalk.setFilter(PathSuffixFilter.create(".java"));
			
			while(treeWalk.next()) {
				String path = treeWalk.getPathString();
				
				paths.add(new PathRevision(path, commit));			
			}
		}
		
		return paths;
	}
	
	private RevsWithPath collectRevsWithSpecificPath(List<PathRevision> paths) throws MissingObjectException, IncorrectObjectTypeException, CorruptObjectException, IOException {
		RevsWithPath revsInPath = new RevsWithPath();
		
		for(PathRevision pr : paths) {
			if(revsInPath.containsKey(pr.getPath())) {
				List<RevCommit> lst = revsInPath.get(pr.getPath());
				lst.add(pr.getCommit());
				revsInPath.replace(pr.getPath(), lst);
			}else{
				List<RevCommit> lst = new ArrayList<>();
				lst.add(pr.getCommit());
				revsInPath.put(pr.getPath(), lst);
			}
		}
		
		return revsInPath;
	}
	
	private void configureLineList(LinkedList<Line> lst, String path, RevCommit rev, String content){
		String[] lineContentArr = content.split("\n");
		
		for(int i = 0; i < lineContentArr.length; i++) {
			String lineContent = lineContentArr[i];
		
			// make new Line
			List<Line> ancestors = new LinkedList<>();
			Line line = new Line(path, rev.getName(), content, i, ancestors); 
			 
			lst.add(line);
		}
		
	}
	
	private ArrayList<Hunk> configureHunkList(EditList editList){
		ArrayList<Hunk> hunkList = new ArrayList<>();
		
		for (Edit edit : editList) {
			Hunk hunk = new Hunk(edit.getType().toString(), 
									edit.getBeginA(), 
									edit.getEndA(), 
									edit.getBeginB(), 
									edit.getEndB());
			
			hunkList.add(hunk);
		}
		
		return hunkList;
	}
}
