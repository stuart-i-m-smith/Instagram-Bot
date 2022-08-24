# Instagram Bot

Auto lists and unfollows non following instagram accounts

## Usage:

Follow new accounts in your app and like their most recent post.

Run this script every few days and if the followed account posts on their timeline without following you back the script will list them as a non follower.

You can then unfollow them manually or get the script to do it too.

Instagram can easily detect programmatic interaction so use at your own risk. For additional safety create a smurf account you dont mind losing to list your non followers.

If you login with the target acocunt you can toggle auto unfollow also.

## Properties keys:
username - the account to login with.
password - the account password to login with.
targetAccount - the account to check for unfollowers.
whitelistpath - ignore some non following accounts you want to keep.
unfollowEnabled - toggle to perform the account unfollow (username must match target account).
