package hgu.csee.isel.alinew.szz;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.diff.DiffAlgorithm;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import hgu.csee.isel.alinew.szz.model.RevsWithPath;
import hgu.csee.isel.alinew.szz.graph.AnnotationGraphBuilder;
import hgu.csee.isel.alinew.szz.graph.AnnotationGraphModel;
import hgu.csee.isel.alinew.szz.model.PathRevision;

public class AgSZZ {
//	private final String GIT_DIR = "/Users/yoon/git/DataForSZZ";
//	private final String GIT_DIR = "/Users/yoon/git/DataForINSERT";
	private final String GIT_DIR = "/Users/yoon/git/DataForDELETE";
//	private final String GIT_DIR = "/Users/yoon/git/BugPatchCollector";
	//private final String FIX_COMMIT = "768b0df07b2722db926e99a8f917deeb5b55d628";
	
	private static Git git;
	private Repository repo;
	
	
	public static void main(String[] args) {
		new AgSZZ().run();	
	}
	
	private void run() {
		try {
			git = Git.open(new File(GIT_DIR));
			repo = git.getRepository();
			List<RevCommit> commits = getCommits(git);
			
			// Phase 1 : Build the annotation graph
			AnnotationGraphBuilder agb = new AnnotationGraphBuilder(repo, commits);
			AnnotationGraphModel agm = agb.buildAnnotationGraph();
			
			// TODO Phase 2 : Trace and collect BIC candidates
			// TODO Phase 3 : Filter out format changes, comments, etc among BIC candidates
			
		} catch (IOException | GitAPIException e) {
			
			e.printStackTrace();
		}
	}
	
	private List<RevCommit> getCommits(Git git) throws NoHeadException, GitAPIException {
		List<RevCommit> commits = new ArrayList<>();
	
		Iterable<RevCommit> logs;
		
		logs = git.log().call();
			
		for(RevCommit rev:logs) {
			commits.add(rev);
		}
		
		return commits;
	}

}
