package com;

import com.github.instagram4j.instagram4j.IGClient;
import com.github.instagram4j.instagram4j.actions.feed.FeedIterable;
import com.github.instagram4j.instagram4j.actions.users.UserAction;
import com.github.instagram4j.instagram4j.models.media.timeline.TimelineMedia;
import com.github.instagram4j.instagram4j.models.user.Profile;
import com.github.instagram4j.instagram4j.requests.feed.FeedUserRequest;
import com.github.instagram4j.instagram4j.requests.feed.FeedUserStoryRequest;
import com.github.instagram4j.instagram4j.requests.friendships.FriendshipsFeedsRequest;
import com.github.instagram4j.instagram4j.responses.feed.FeedUserResponse;
import com.github.instagram4j.instagram4j.responses.feed.FeedUserStoryResponse;
import com.github.instagram4j.instagram4j.responses.feed.FeedUsersResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static void main(String[] args) throws Exception {
        Properties properties;
        try (InputStream input = new FileInputStream(args[0])) {
            properties = new Properties();
            properties.load(input);
        }

        IGClient client = IGClient.builder()
                .username(properties.getProperty("username"))
                .password(properties.getProperty("password"))
                .login();

        UserAction userAction = client.actions().users().findByUsername("that.mr.smith").get();

        FeedIterable<FriendshipsFeedsRequest, FeedUsersResponse> followers = userAction.followersFeed();

        List<Profile> followersUsers = new ArrayList<>();
        for(var c : followers) {
            followersUsers.addAll(c.getUsers());
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(2));
        }

        FeedIterable<FriendshipsFeedsRequest, FeedUsersResponse> following = userAction.followingFeed();

        List<Profile> followingUsers = new ArrayList<>();
        for(var c : following) {
            followingUsers.addAll(c.getUsers());
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(2));
        }

        List<Profile> nonFollowers = new ArrayList<>();
        Set<String> whiteListAccounts = Files.lines(Paths.get("src/main/resources/accountswhitelist.txt")).collect(Collectors.toSet());

        for(Profile followingUser : followingUsers){

            boolean isFollowing = followersUsers.stream()
                    .anyMatch(profile -> followingUser.getUsername().equals(profile.getUsername()));

            if(!isFollowing && !whiteListAccounts.contains(followingUser.getUsername())){
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(2));

                FeedUserResponse userFeed = new FeedUserRequest(followingUser.getPk()).execute(client).join();
                List<TimelineMedia> posts = userFeed.getItems();

                if(posts.size() > 0) {
                    List<Map<String, Object>> likers = (List<Map<String, Object>>)posts.get(0).getExtraProperties().get("likers");

                    boolean hasPostedSinceLiked = likers == null ||
                            likers.stream().noneMatch(liker -> "that.mr.smith".equals(liker.get("username")));

                    if(hasPostedSinceLiked) {
//                        FeedUserStoryResponse userStories = new FeedUserStoryRequest(followingUser.getPk()).execute(client).join();
//                        if(userStories.getReel().getItems().size() > 0)
                        nonFollowers.add(followingUser);
                    }
                }
            }

        }

        LOGGER.info("Non followers that have posted since following:");
        nonFollowers.forEach(profile -> LOGGER.info(profile.getUsername()));
        LOGGER.info("======");
    }
}
