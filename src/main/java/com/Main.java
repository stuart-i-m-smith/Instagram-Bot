package com;

import com.github.instagram4j.instagram4j.IGClient;
import com.github.instagram4j.instagram4j.actions.feed.FeedIterable;
import com.github.instagram4j.instagram4j.actions.users.UserAction;
import com.github.instagram4j.instagram4j.models.media.timeline.TimelineMedia;
import com.github.instagram4j.instagram4j.models.user.Profile;
import com.github.instagram4j.instagram4j.models.user.User;
import com.github.instagram4j.instagram4j.requests.feed.FeedUserRequest;
import com.github.instagram4j.instagram4j.requests.friendships.FriendshipsActionRequest;
import com.github.instagram4j.instagram4j.requests.friendships.FriendshipsFeedsRequest;
import com.github.instagram4j.instagram4j.responses.feed.FeedUserResponse;
import com.github.instagram4j.instagram4j.responses.feed.FeedUsersResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static void main(String[] args) throws Exception {
        Properties properties;
        try (InputStream input = new FileInputStream(args[0])) {
            properties = new Properties();
            properties.load(input);
        }

        String targetAccount = properties.getProperty("targetAccount");

        IGClient client = IGClient.builder()
                .username(properties.getProperty("username"))
                .password(properties.getProperty("password"))
                .login();

        UserAction userAction = client.actions().users().findByUsername(targetAccount).get();

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
        Set<String> whiteListAccounts = Files.lines(Paths.get(properties.getProperty("whitelistpath"))).collect(Collectors.toSet());

        for(Profile followingUser : followingUsers){

            boolean isFollowing = followersUsers.stream()
                    .anyMatch(profile -> followingUser.getUsername().equals(profile.getUsername()));

            if(!isFollowing && !whiteListAccounts.contains(followingUser.getUsername())){
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(2));

                FeedUserResponse userFeed = new FeedUserRequest(followingUser.getPk()).execute(client).join();
                List<TimelineMedia> posts = userFeed.getItems();

                if(posts.size() > 0) {
                    boolean hasPostedSinceLiked = hasPostedSinceLiked(targetAccount, posts.get(0), targetAccount.equals(properties.getProperty("username")));

                    if(hasPostedSinceLiked) {
                        nonFollowers.add(followingUser);
                    }
                }
            }
        }

        LOGGER.info("Non followers that have posted since following:");
        nonFollowers.forEach(profile -> LOGGER.info(profile.getUsername()));
        LOGGER.info("======");

        if(Boolean.parseBoolean(properties.getProperty("unfollowEnabled")) && targetAccount.equals(properties.getProperty("username"))){
            nonFollowers.forEach(profile -> {
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
                CompletableFuture<User> info = client.actions().users().info(profile.getPk());
                try {
                    User user = info.get();
                    LOGGER.info("Unfollowing <{}>", user.getUsername());
                    LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
                    new UserAction(client, user).action(FriendshipsActionRequest.FriendshipsAction.REMOVE_FOLLOWER);
                } catch (Exception e) {
                    LOGGER.error("Problem unfollowing user", e);
                }
            });
        }
    }

    private static boolean hasPostedSinceLiked(String targetAccount, TimelineMedia post, boolean isClientTarget){
        if(isClientTarget){
            return post.isHas_liked();
        }

        List<Map<String, Object>> likers = (List<Map<String, Object>>)post.getExtraProperties().get("likers");

        return likers == null ||
                likers.stream().noneMatch(liker -> targetAccount.equals(liker.get("username")));
    }
}
