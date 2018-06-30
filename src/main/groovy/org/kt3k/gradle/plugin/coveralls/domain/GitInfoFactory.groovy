package org.kt3k.gradle.plugin.coveralls.domain

import org.eclipse.jgit.lib.*
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk

/**
 * GitInfoFactory is factory class of GitInfo.
 * This class is based on GitRepository from https://github.com/trautonen/coveralls-maven-plugin
 */
class GitInfoFactory {

    /**
     * Create GitInfo instance from directory.
     *
     */
    public static GitInfo load(File sourceDirectory, Map<String, String> env) throws IOException {
        Repository repository = buildRepository(sourceDirectory)
        if (repository != null) {
            try {
                return new GitInfo(
                        head: getHead(repository, env),
                        branch: env.get('CI_BRANCH') ?: getBranch(repository),
                        remotes: getRemotes(repository));
            } finally {
                repository.close();
            }
        } else {
            return null
        }

    }

    private static GitInfo.Head getHead(final Repository repository, Map<String, String> env) throws IOException {
        ObjectId revision = repository.resolve(Constants.HEAD);
        RevCommit commit = new RevWalk(repository).parseCommit(revision);
        GitInfo.Head head = new GitInfo.Head(
                id: env.get('COVERALLS_GIT_COMMIT') ?: revision.getName(),
                authorName: commit.getAuthorIdent().getName(),
                authorEmail: commit.getAuthorIdent().getEmailAddress(),
                committerName: commit.getCommitterIdent().getName(),
                committerEmail: commit.getCommitterIdent().getEmailAddress(),
                message: commit.getFullMessage()
        );
        return head;
    }

    private static String getBranch(final Repository repository) throws IOException {
        return repository.getBranch();
    }

    private static List<GitInfo.Remote> getRemotes(final Repository repository) {
        Config config = repository.getConfig();
        List<GitInfo.Remote> remotes = new ArrayList<GitInfo.Remote>();
        for (String remote : config.getSubsections("remote")) {
            String url = config.getString("remote", remote, "url");
            remotes.add(new GitInfo.Remote(name: remote, url: url));
        }
        return remotes;
    }

    private static Repository buildRepository(File repoUri) {
        def repositoryBuilder = new RepositoryBuilder().findGitDir(repoUri);
        if (repositoryBuilder.getGitDir() == null && repositoryBuilder.getWorkTree() == null) {
            return null
        } else {
            return repositoryBuilder.build()
        }
    }
}
