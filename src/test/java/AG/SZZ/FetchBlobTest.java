package AG.SZZ;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.LargeObjectException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

import hgu.csee.isel.alinew.szz.util.Utils;

public class FetchBlobTest {
	
	public static void main(String[] args) throws IOException, NoHeadException, GitAPIException {
		Git git = Git.open(new File("/Users/kimseokjin/git/incubator-iotdb"));
		String path = "src/test/java/cn/edu/thu/tsfiledb/metadata/MManagerTest.java";
		Repository repo = git.getRepository();
		
		Iterable<RevCommit> logs = git.log().call();

		for (RevCommit commit : logs) {
			if(commit.getName().equals("0db49a91512ccbe93c425217a6dd4d0291796322")) {
				System.out.println("HEADER");
				System.out.println(Utils.removeComments(fetchBlob(repo, commit, path)).trim());
				System.out.println("FOOTER");
				
				System.out.println("HEADER");
				System.out.println(fetchBlob(repo, commit, path));
				System.out.println("FOOTER");
			}
		}

	}
	
	public static String fetchBlob(Repository repo, RevCommit commit, String path)
			throws LargeObjectException, MissingObjectException, IOException {

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
	
	public static String fetchBlob(Repository repo, String revSpec, String path)
			throws RevisionSyntaxException, AmbiguousObjectException, IncorrectObjectTypeException, IOException {

		// Resolve the revision specification
		final ObjectId id = repo.resolve(revSpec);

		// Makes it simpler to release the allocated resources in one go
		ObjectReader reader = repo.newObjectReader();

		// Get the commit object for that revision
		RevWalk walk = new RevWalk(reader);
		RevCommit commit = walk.parseCommit(id);
		walk.close();

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
}
