package hgu.csee.isel.alinew.szz.graph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import hgu.csee.isel.alinew.szz.model.Hunk;
import hgu.csee.isel.alinew.szz.model.Line;
import hgu.csee.isel.alinew.szz.model.LineType;
import hgu.csee.isel.alinew.szz.model.PathRevision;
import hgu.csee.isel.alinew.szz.model.RevsWithPath;
import hgu.csee.isel.alinew.szz.util.Utils;

public class AnnotationGraphBuilder {
	private Repository repo;
	private List<RevCommit> commits;
	private PathRevision childPathRev;
	private PathRevision parentPathRev;

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
		List<PathRevision> paths = configurePathRevisionList(repo, commits);
		RevsWithPath revsWithPath = configureRevsWithPath(paths);
		
		Iterator<String> keys = revsWithPath.keySet().iterator();
		while( keys.hasNext() ){
			String path = keys.next();
//			System.out.println("\tkey : " + key);
			List<RevCommit> lst = revsWithPath.get(path);
			for(RevCommit child : lst) {
				if(lst.indexOf(child) == lst.size()-1) break;
				
				RevCommit parent = lst.get(lst.indexOf(child)+1);
				
				childPathRev = new PathRevision(path, child);
				parentPathRev = new PathRevision(path, parent);
						
				// Diff
				DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
				df.setRepository(repo);
				df.setPathFilter(PathFilter.create(path));
				List<DiffEntry> diffs = df.scan(parent.getTree(), child.getTree());
				
				
				for(DiffEntry diff : diffs) {

					// get preFixSource and fixSource without comments
					String parentFileSource = Utils.fetchBlob(repo, parent, path);
					String childFileSource = Utils.fetchBlob(repo, child, path);
					
					System.out.println("prev: "+parentFileSource);
					System.out.println("current: "+childFileSource);
					
					String[] parentContentList = parentFileSource.split("\n");
					String[] childContentList = childFileSource.split("\n");
					
					LinkedList<Line> parentLineList = new LinkedList<>();
					LinkedList<Line> childLineList = new LinkedList<>();
					
					for(int i = 0; i < parentContentList.length; i++) {
						String content = parentContentList[i];
						List<Line> ancestors = new ArrayList<>();
						Line line = new Line(path, child.getName(), content, i, ancestors);
						parentLineList.add(line);
					}
					
					for(int i = 0; i < childContentList.length; i++) {
						String content = childContentList[i];
						List<Line> ancestors = new ArrayList<>();
						Line line = new Line(path, child.getName(), content, i, ancestors);
						childLineList.add(line);
					}
					
					// TEST
//					System.out.println("=======================================parent========================================");					
//					
//					System.out.println("line path: " + parentLineList.get(0).getPath());
//					System.out.println("line rev: " + parentLineList.get(0).getRev());
//					
//					for(Line line: parentLineList) {
//						System.out.println("line idx: " + line.getIdx());
//						System.out.println("line content: " + line.getContent());						
//					}
//					
//					
//					System.out.println("=======================================child========================================");
//					
//					System.out.println("line path: " + childLineList.get(0).getPath());
//					System.out.println("line rev: " + childLineList.get(0).getRev());
//					
//					for(Line line: childLineList) {
//						System.out.println("line idx: " + line.getIdx());
//						System.out.println("line content: " + line.getContent());						
//					}
//					
//					
//					System.out.println("=======================================done========================================");
					
					// get line indices that are related to BI lines.
					EditList editList = Utils.getEditListFromDiff(parentFileSource, childFileSource);
					ArrayList<Hunk> hunkList = new ArrayList<>();
					for (Edit edit : editList) {

						int beginA = edit.getBeginA();
						int endA = edit.getEndA();
						int beginB = edit.getBeginB();
						int endB = edit.getEndB();

						Hunk hunk = new Hunk(edit.getType().toString(), beginA, endA, beginB, endB);
						hunkList.add(hunk);
						
						// TEST
						System.out.println("Type : " + hunk.getDiffType());
						System.out.println("beginA : " + hunk.getBeginA());
						System.out.println("endA : " + hunk.getEndA());
						System.out.println("beginB : " + hunk.getBeginB());
						System.out.println("endB : " + hunk.getEndB());
//						System.out.println("rangeA : " + hunk.getRangeA());
//						System.out.println("rangeA : " + hunk.getRangeB());
						System.out.println("");

					}

					int idx = 0, hunkIdx = 0, offset = 0;
					
					// search each line
					while(idx < childLineList.size()) {
						
						Line line = childLineList.get(idx);
						
						if( hunkList.size() <= hunkIdx) {
							Line ancestor = parentLineList.get(idx + offset);
							
							if(line.getAncestors().size() == 0) {
								line.setAncestors(new ArrayList<Line>());
							}
							
							List<Line> ancestors = line.getAncestors();
							ancestors.add(ancestor);
							line.setAncestors(ancestors);
							idx++;
							continue;
						}
//						System.out.println("hunk idx: "+hunkIdx);
						Hunk hunk = hunkList.get(hunkIdx);
						
						int begin = hunk.getBeginB();
						int end = hunk.getEndB();
						
						if(idx <hunk.getBeginB()) {
							// 해당 hunk의 beginB보다 작을 때까지
							// context
							Line ancestor = parentLineList.get(idx + offset);
							
							if(line.getAncestors().size() == 0) {
								line.setAncestors(new ArrayList<Line>());
							}
							
							List<Line> ancestors = line.getAncestors();
							ancestors.add(ancestor);
							line.setAncestors(ancestors);
							
						} else if(begin <= idx && idx < end) {
							//해당 hunk범위
							// insert, delete, replace
							String hunkType = hunk.getDiffType();
							
							switch(hunkType) {
								case "INSERT" :
									line.setLineType(LineType.INSERT);
									offset--;
									break;
								case "DELETE" :
									line.setLineType(LineType.DELETE);
									offset++;
									break;
								case "REPLACE" :
									line.setLineType(LineType.REPLACE);
									
									List<Line> ancestors = parentLineList.subList(hunk.getBeginA(), hunk.getEndA());
									line.setAncestors(ancestors);
									
									// setting
//									for(int i = hunk.getBeginA(); i < hunk.getRangeA(); i++) {
//										Line ancestor = parentLineList.get(offset + i);
//										
//										if(line.getAncestors().size() == 0) {
//											line.setAncestors(new ArrayList<>());
//										}
//										
//										List<Line> ancestors = line.getAncestors();
//										ancestors.add(ancestor);
//										line.setAncestors(ancestors);
//									}
									
									//offset이 중복으로 더해지는 걸 방지  
									if(idx == hunk.getRangeB()) {
										offset += hunk.getRangeA()-hunk.getRangeB();	
										hunkIdx++;
									}
									
//									idx += hunk.getRangeA();
									break;
								default : 
									System.err.println("ERROR");
							
							}
						
							
						}
						
						idx++;
					}// while done
					
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
					
					
				}
			}
		}
		
		return null;
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
	
	private RevsWithPath configureRevsWithPath(List<PathRevision> paths) throws MissingObjectException, IncorrectObjectTypeException, CorruptObjectException, IOException {
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

}
