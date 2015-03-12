package bitbucketpullrequestbuilder.bitbucketpullrequestbuilder;

import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.model.Result;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by nishio
 */
public class BitbucketBuilds {
    private static final Logger logger = Logger.getLogger(BitbucketBuilds.class.getName());
    private BitbucketBuildTrigger trigger;
    private BitbucketRepository repository;

    public BitbucketBuilds(BitbucketBuildTrigger trigger, BitbucketRepository repository) {
        this.trigger = trigger;
        this.repository = repository;
    }

    public BitbucketCause getCause(AbstractBuild build) {
        Cause cause = build.getCause(BitbucketCause.class);
        if (cause == null || !(cause instanceof BitbucketCause)) {
            return null;
        }
        return (BitbucketCause) cause;
    }

    public void onStarted(AbstractBuild build) {
        BitbucketCause cause = this.getCause(build);
        if (cause == null) {
            return;
        }
        try {
            build.setDescription(cause.getShortDescription());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Can't update build description", e);
        }
    }

    public void onCompleted(AbstractBuild build) {
        BitbucketCause cause = this.getCause(build);
        if (cause == null) {
            return;
        }
        Result result = build.getResult();
        String rootUrl = Jenkins.getInstance().getRootUrl();
        String buildUrl = "";
        if (rootUrl == null) {
            buildUrl = " PLEASE SET JENKINS ROOT URL FROM GLOBAL CONFIGURATION " + build.getUrl();
        }
        else {
            buildUrl = rootUrl + build.getUrl();
        }
        repository.deletePullRequestComment(cause.getPullRequestId(), cause.getBuildStartCommentId());

        String commentFilePath = trigger.getPostBuildCommentFilePath();
        if (commentFilePath != null && !commentFilePath.isEmpty()) {
          // build now has workspace path, time to use it when posting the post-build comment (toString is deprecated)
          repository.postPostBuildCommentFromFile(cause.getPullRequestId(), build.getWorkspace().toString() + "/" + commentFilePath, buildUrl);
        }

        repository.postFinishedComment(cause.getPullRequestId(), cause.getSourceCommitHash(), cause.getDestinationCommitHash(), result == Result.SUCCESS, buildUrl);

    }
}
