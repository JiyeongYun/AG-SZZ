package hgu.csee.isel.alinew.szz.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.diff.DiffAlgorithm;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.errors.LargeObjectException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import hgu.csee.isel.alinew.szz.model.Line;

public class Utils {
	
	public static DiffAlgorithm diffAlgorithm = DiffAlgorithm.getAlgorithm(DiffAlgorithm.SupportedAlgorithm.MYERS);
	public static RawTextComparator diffComparator = RawTextComparator.DEFAULT;
	
	public static EditList getEditListFromDiff(String file1, String file2) {
		RawText rt1 = new RawText(file1.getBytes());
		RawText rt2 = new RawText(file2.getBytes());
		EditList diffList = new EditList();

		diffList.addAll(diffAlgorithm
				.diff(diffComparator, rt1, rt2));
		return diffList;
	}
	
	public static String fetchBlob(Repository repo, RevCommit commit, String path) throws LargeObjectException, MissingObjectException, IOException {

		// Makes it simpler to release the allocated resources in one go
		ObjectReader reader = repo.newObjectReader();

		// Get the revision's file tree
		RevTree tree = commit.getTree();
		// .. and narrow it down to the single file's path
		TreeWalk treewalk = TreeWalk.forPath(reader, path, tree);

		if (treewalk != null) {
			// use the blob id to read the file's data
			byte[] data = reader.open(treewalk.getObjectId(0)).getBytes();
			reader.close();
			return new String(data, "utf-8");
		} else {
			return "";
		}
	}
	
	public static List<RevCommit> getRevs(Git git) throws NoHeadException, GitAPIException {
		List<RevCommit> commits = new ArrayList<>();
	
		Iterable<RevCommit> logs;
		
		logs = git.log().call();
			
		for(RevCommit rev:logs) {
			commits.add(rev);
		}
		
		return commits;
	}
	
	public static List<DiffEntry> diff(Repository repo, RevTree parentTree, RevTree childTree) throws IOException{
		List<DiffEntry> diffs;
		
		DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
		df.setRepository(repo);
		df.setDiffAlgorithm(Utils.diffAlgorithm);
		df.setDiffComparator(Utils.diffComparator);
		df.setPathFilter(PathSuffixFilter.create(".java"));
		
		diffs = df.scan(parentTree, childTree);
		
		df.close();
		
		return diffs;
	}
	
	public static boolean isWhitespace(String str) {
		return str.replaceAll("\\s", "").equals("");
	}
	
	public static String mergeLineList(List<Line> list) {
		String mergedContent = "";
		
		for(Line line : list) {
			mergedContent += line.getContent();
		}
		
		return mergedContent.replaceAll("\\s", "");	
	}
}
