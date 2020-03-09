package hgu.csee.isel.alinew.szz;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
import hgu.csee.isel.alinew.szz.util.Utils;

public class AGSZZ {
	private String GIT_URL;
	private List<String> BFCList = new ArrayList<>();
	private boolean debug;
	private File localPath;

	public AGSZZ(String gIT_URL, List<String> bFCList, boolean debug) {
		this.GIT_URL = gIT_URL;
		this.BFCList = bFCList;
		this.debug = debug;
	}

	public void run() throws IOException {
		try {
			// Clone
			final String REMOTE_URI = GIT_URL + ".git";
			
			// prepare a new folder for the cloned repository
			localPath = File.createTempFile("TestGitRepository", "");
			if (!localPath.delete()) {
				throw new IOException("Could not delete temporary file " + localPath);
			}
			
			System.out.println("\nCloning from " + REMOTE_URI + " to " + localPath);

			Git git = Git.cloneRepository()
						 .setURI(REMOTE_URI)
						 .setDirectory(localPath)
						 .call();

			System.out.println("Having repository: " + git.getRepository().getDirectory());

			Repository repo = git.getRepository();
			
			List<RevCommit> revs = GitUtils.getRevs(git);
			
			List<RevCommit> bfcList = GitUtils.getBFCList(BFCList, revs);
			
			List<String> targetPaths = GitUtils.getTargetPaths(repo, bfcList);
			
			RevsWithPath revsWithPath = GitUtils.collectRevsWithSpecificPath(GitUtils.configurePathRevisionList(repo, revs), targetPaths);
			
			// Phase 1 : Build the annotation graph
			final long startBuildingTime = System.currentTimeMillis();

			AnnotationGraphBuilder agb = new AnnotationGraphBuilder();
			AnnotationGraphModel agm = agb.buildAnnotationGraph(repo, revsWithPath, debug);

			final long endBuildingTime = System.currentTimeMillis();
			System.out.println("\nBuilding Annotation Graph takes " + (endBuildingTime - startBuildingTime) / 1000.0 + "s\n");

			// Phase 2 : Trace and collect BIC candidates and filter out format changes, comments, etc among candidates
			final long startTracingTime = System.currentTimeMillis();
			
			Tracer tracer = new Tracer();
			//List<Line> BILines = tracer.collectBILines(repo, revs, agm, revsWithPath, BFCList, debug);
			List<Line> BILines = tracer.collectBILines(repo, bfcList, agm, revsWithPath, debug);
			
			final long endTracingTime = System.currentTimeMillis();
			System.out.println("\nCollecting BICs takes " + (endTracingTime - startTracingTime) / 1000.0 + "s\n");
			
			// Phase 3 : store outputs
			Utils.storeOutputFile(GIT_URL, BILines);

		} catch (IOException | GitAPIException | EmptyHunkTypeException e) {
			e.printStackTrace();
		} finally {
			// clean up here to not keep using more and more disk-space for these samples
			FileUtils.deleteDirectory(localPath);
			System.out.println("Clean up " + localPath);
		}
	}
}
