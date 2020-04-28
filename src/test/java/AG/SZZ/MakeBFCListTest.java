package AG.SZZ;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import hgu.csee.isel.alinew.szz.util.GitUtils;

public class MakeBFCListTest {

	public MakeBFCListTest() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) throws IOException, NoHeadException, GitAPIException {
		String path = "/Users/kimseokjin/git/zeppelin";
		File file = new File(path);
		
		Git git = Git.open(file);
		Repository repo = git.getRepository();
		
		List<RevCommit> revs = GitUtils.getRevs(git);
		
		String filePath = "/Users/kimseokjin/git/JiraCrawler/apacheZEPPELIN/apacheZEPPELINIssueKeys2020-04-16 14:12:46.626.csv";
		
		List<String> issueKeys = Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);
		
		ArrayList<RevCommit> BFCList = getBFC(issueKeys, revs);
		
		//TEST
		for(RevCommit BFC : BFCList) {
			System.out.println(BFC.getShortMessage());
		}
		

	}
	
	public static ArrayList<RevCommit> getBFC(List<String> issueKeys, List<RevCommit> revs) {
		ArrayList<RevCommit> BFCList = new ArrayList<RevCommit>();
	
		for(String issueKey : issueKeys) {
			ArrayList<RevCommit> temp = new ArrayList<RevCommit>();
			
			for(RevCommit rev : revs) {
				if(rev.getShortMessage().contains(issueKey)) {
					temp.add(rev);
				}
			
			}
			// A BFC is a commit that is mentioned exactly once for the corresponding issue key in the entire commit.
			if(temp.size() == 1) {
				BFCList.addAll(temp);
			}
		}
		
		return BFCList;
	}

}
