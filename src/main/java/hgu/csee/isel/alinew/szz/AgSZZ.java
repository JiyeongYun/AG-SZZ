package hgu.csee.isel.alinew.szz;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import hgu.csee.isel.alinew.szz.exception.EmptyHunkTypeException;
import hgu.csee.isel.alinew.szz.graph.AnnotationGraphBuilder;
import hgu.csee.isel.alinew.szz.graph.AnnotationGraphModel;
import hgu.csee.isel.alinew.szz.model.Line;
import hgu.csee.isel.alinew.szz.model.RevsWithPath;
import hgu.csee.isel.alinew.szz.trace.Tracer;
import hgu.csee.isel.alinew.szz.util.GitUtils;

public class AgSZZ {
//	private final String REMOTE_URL = "https://github.com/apache/pulsar.git";
	private final String FIX_COMMIT = "4264b8d42cead9c52dd90cf9656675acd7d9fa45";
	private List<String> BFCList = new ArrayList<>();
	
	private File localPath;

	public static void main(String[] args) throws IOException {
		new AgSZZ().run();
	}

	private void run() throws IOException {
		try {
//			// prepare a new folder for the cloned repository
//			localPath = File.createTempFile("TestGitRepository", "");
//			if (!localPath.delete()) {
//				throw new IOException("Could not delete temporary file " + localPath);
//			}
//
//			// then clone
//			System.out.println("Cloning from " + REMOTE_URL + " to " + localPath);
//
//			Git git = Git.cloneRepository()
//						.setURI(REMOTE_URL).
//						setDirectory(localPath).
//						call();
//
//			System.out.println("Having repository: " + git.getRepository().getDirectory());
			
			Git git = Git.open(new File("/Users/kimseokjin/git/netbeans"));
						
			Repository repo = git.getRepository();
			List<RevCommit> revs = GitUtils.getRevs(git);
			BFCList.add(FIX_COMMIT);

			RevsWithPath revsWithPath = GitUtils
					.collectRevsWithSpecificPath(GitUtils.configurePathRevisionList(repo, revs));

			// Phase 1 : Build the annotation graph
			final long startTime = System.currentTimeMillis();

			AnnotationGraphBuilder agb = new AnnotationGraphBuilder();
			AnnotationGraphModel agm = agb.buildAnnotationGraph(repo, revsWithPath, false);

			final long endTime = System.currentTimeMillis();
			System.out.println("Building Annotation Graph takes " + (endTime - startTime) / 1000.0 + "s");
			
			// TEST
//			Iterator<RevCommit> commits = agm.keySet().iterator();
//			
//			int revCnt = 1;
//			while(commits.hasNext()) {
//				
//				RevCommit commit = commits.next();
//				
//				System.out.println("\nRev Count : " + revCnt + "=========================\n");
//				
//				System.out.println("rev : " + commit.getName());
//				HashMap<String, ArrayList<Line>> subAnnotationGraph = agm.get(commit);
//				Iterator<String> paths = subAnnotationGraph.keySet().iterator();
//				
//				while(paths.hasNext()) {
//					String path = paths.next();
//					System.out.println("path : " + path);
//					
////					ArrayList<Line> lines = subAnnotationGraph.get(path);
////					
////					for(Line l : lines) {
////						System.out.println("\trev : " + l.getRev());
////						System.out.println("\tpath : " + l.getPath() + "\n\n");
////						
////						System.out.println("content : " + l.getContent());
////						System.out.println("index : " + l.getIdx());
////						System.out.println("type : " + l.getLineType() + "\n") ;
////					}
//				}
//				
//				revCnt++;
//			}

			// Phase 2 : Trace and collect BIC candidates
			// Phase 3 : Filter out format changes, comments, etc among BIC candidates
			Tracer tracer = new Tracer();
			List<Line> BILines = tracer.collectBILines(repo, revs, agm, revsWithPath, BFCList, true);

			// TEST
			System.out.println("\nsize: " + BILines.size());
			for (Line line : BILines) {
				System.out.println("BIC: " + line.getIdx());
				System.out.println("Path: " + line.getPath());
				System.out.println("Revision: " + line.getRev());
				System.out.println("Content: " + line.getContent() + "\n");
			}

		} catch (IOException | GitAPIException | EmptyHunkTypeException e) {
			e.printStackTrace();
		} finally {
			// clean up here to not keep using more and more disk-space for these samples
//			FileUtils.deleteDirectory(localPath);
//			System.out.println("Clean up " + localPath);
		}
	}
}
