# Hackathon 2026s1: moderation tools

We implemented Admin users back in week 3 of the miniproject, but they don't have any functionality. Today, we will implement additional moderation tools to help Admins. Precisely speaking, we are interested in a program flow where Users may report messages. Admins are then able to review reported messages, and hide messages that contain e.g. abusive content. This additional functionality has some skeleton code provided in the module `moderation`.

As a group, you must decide on the architecture for your program. You are provided a copy of the miniproject from week 5, with  solutions to some lab tasks, to use as a base.

Follow the following guidelines when writing code:
- You may write your code anywhere within the `app/src` folder, including by adding new files and classes.
- Do not modify the contents of the `ReactionType` enum.

To reiterate, you may augment existing code, including that which is unrelated to the hackathon, as you please. For example, if you want to implement a red-black tree, you may create a new package within `app/src/persistentdata` to do so.

### Getting started

It is necessary to make some design decisions about the additional module's architecture. We strongly recommend you begin by skimming each of the tasks, not just your own, to understand how the module will come together. Then, you should discuss the architecture *as a group*, considering questions such as:

- How will we abstractly represent a report?
- Which data structures will we use to store multiple reports?
- How will we represent a hidden message?
- How can we store these types of data persistently?
- What features are bottlenecks? In other words, which members of the group must finish certain aspects of their task before others can begin?
- Are any aspects of the design important to allow you to complete your task efficiently (in terms of programming time, runtime, or memory usage)? You may need to negotiate some trade-offs with other members here.

Only once you have reached a consensus about design should you begin programming.

### Git and submission notes

As a group, please make a single fork of the `hackathon` repo. When forking, please leave the project name and slug unchanged, and set the visibility to private.

In the GitLab web view for your fork, go to **Manage** > **Members** in the sidebar. Check that the COMP2100 marker bot has been added (let staff know if it hasn't), and add the accounts of each of the other group members. You will want to give them **Maintainer** permissions, so that they can freely push to the repo.

Each group member should then clone this fork to their personal device to work on the hackathon. You will need to set up your clone in IntelliJ by setting the SDK and importing JUnit, like in the first part of the miniproject.

You may use Git branches as you please throughout your group, but assessment will be based purely on the final commit to the `main` branch before the deadline. Git etiquette will not be considered during marking.

Your code **must** compile to receive marks.

## Task information

### Task 1: reporting functionality (software design, data structures)

Any User will be able to report any Message for moderators to review.

\[50%] Reports are transmitted to the application through a call to `boolean ModerationTools.addReport(UUID message, UUID user, long timestamp)` which returns true if the report was successfully received. If the message or user does not exist, or if that user has already reported that message, do nothing and return false.

They are also able to retract their reports through `ModerationTools.removeReport`. This returns true if the report was successfully removed, and false if the user was not reporting that message or if either UUID does not exist. There is no need to maintain a record of which Users have removed reports.

You must also implement the function `ModerationTools.hasReported`, which checks whether the given user has reported the given message.

Note that even if a message is 'hidden' by a moderator, the reports attached to it should still remain in memory.

\[30%] Like the rest of our application, performance is important. You should choose an appropriate architecture to store reports, with particular consideration towards the use of classes and data structures to maximise efficiency. This performance requirement applies only to the code written for this task, not any pre-existing code in the project.

\[20%] Your code should be high quality.

### Task 2: hiding messages functionality (data structures, trees)

Admin users should be able to hide and unhide Messages at will for moderation purposes. Hidden messages will not be visible to non-Admin users.

\[40%] Implement the function `ModerationTools.setHidden(UUID message, UUID user, boolean hidden)`. This function should check that the UUIDs exist and that the corresponding User is an Admin. If these checks fail, do nothing and return false. Otherwise, update the message's state according to the hidden parameter. Note that when a message is first posted, it is visible by default.

\[40%] You must also update the message-fetching logic to react to Messages changing visibility. In particular, Admin users should always be able to see messages, but other Users and Guests should only be able to see non-hidden messages. To implement this, we have created the function `Post.getVisibleMessages(boolean isAdmin)`. You should implement this function to return a SortedData that, if isAdmin is true, contains all the messages to that post; if isAdmin is false, only the non-hidden messages should be included.

\[20%] Your code should be high quality.

### Task 3: Persistence and refactoring

\[60%] The functionality for both task 1 (user reports) and task 2 (hiding messaages) should persist across runs of the application. There are no methods within the provided ModerationTools interface that must be implemented; instead, you must modify the existing code for persistence to support the changes made in Task 1 and Task 2.

The data written to persistent storage for this task will also be read by other applications, some of which will be written in programming languages other than Java. Therefore, when choosing how to represent this data, you should select a portable representation.

\[40%] The code that you write for this task must be high quality, beyond the level expected for the other tasks. Check your code for any remaining code smells and refactor if they are present.

### Task 4: viewing reports (design patterns)

Moderators should be able to view reports submitted by Users in order to act on them.

\[40%] To do this, implement the function `Iterator<Message> ModerationTools.getReportedMessages(String strategy, int amount)`
This function expects that strategy is either "OLDEST" or "MOST" and that amount is a positive integer. You should throw an exception otherwise.

If the strategy is "OLDEST", return the reported Messages ordered by the timestamp of their oldest non-removed report. That is, the post with the oldest report should be returned first. If the strategy is "MOST", then the returned Messages should be sorted by the number of active (non-removed) reports on them. That is, the post with the most active reports should be returned first.

If there is a tie in the relevant ordering -- meaning two Messages have oldest reports with the same timestamp, or the same number of reports -- you may return those Messages in an arbitrary order.

Do not return Messages that have zero active reports. You should return the specified amount of messages, or fewer if there are insufficiently many reported Messages. If a message has been reported multiple times, it should only be included at most once in the output.

\[40%] It is mandatory that you use both the Iterator and Factory patterns while implementing this task. You must decide where and how these patterns can most appropriately be implemented.

\[20%] Your code should be high quality.

### Task 5: unit testing

In this task, you will use JUnit4 to write test cases for some of the functionality implemented for this hackathon.

\[40%] Write unit tests that achieve branch-complete coverage on `ModerationTools.addReport`, including any submethods called from addReport that were written by your team in this hackathon. Refer to Task 1 for the specification of this function. Write these tests in the ModerationToolsAddReportTests class in the unit test folder.

\[40%] Write black-box unit tests for the method `ModerationTools.getReportedMessages`. Your tests should be able to distinguish between a correct and faulty implementation of getReportedMessages, assuming that all other functionality is correct. Refer to Task 4 for the specification of this function. Write these tests in the ModerationToolsGetReportsTests class.

These tests do not need to be parameterised; please test only the implementation in the codebase, using only `getReportedMessages` as an entry point for testing.

\[20%] Your code should be high quality.

### Group task (UML)

Produce a UML diagram illustrating the architecture of the moderation tools.

\[50%] At minimum, your UML diagram should include at least
- five classes, including the ModerationTools class
- one private, one protected, and one public field
- two static and two non-static methods
- one example of each of composition, aggregation, and association

\[50%] Your UML diagram should be informative and useful to a reader who wants to understand the architecture of your project. This means the selection of features should be wide enough to show the reader the overall architecture but narrow enough that irrelevancies are excluded, and they should be arranged well.

You can either draw your diagram on paper and take a photo, or use online tools and take a screenshot. Once you've finished, upload this photo or screenshot to your Git fork by replacing the file `uml.png`.