# Homework 2

## Name: Sai Nadkarni
## UIN: 672756678
## NetID: snadka2
## Instructions and documentation:

## Project set up
+ Clone the project or download the repo in a zip format
+ Open a terminal at the root level of the repository
+ To run the test cases written for the simulations,

```
sbt clean compile test
```

+ To run the log generator

```
sbt clean compile run
```
+ To run the M/R jobs

```
sbt clean compile assembly
(in appropriate hadoop fs) hadoop fs jar-file.jar input/ output/
```

### Explanation video:

Google Drive- https://drive.google.com/file/d/1R5ZtMwe08oJCvnkmx12b8ey7N8dptQY-/view?usp=sharing
YouTube- https://youtu.be/3HQkevVeOUk

Includes explanation of code as well.

### The goal of this homework is for students to gain experience with solving a distributed computational problem using cloud computing technologies. The main textbook group (option 1) will design and implement an instance of the map/reduce computational model whereas the alternative textbook group (option 2) will use the CORBA model. You can check your textbook option in the corresponding column of the gradebook on the Blackboard.
### Grade: 9%
#### This Git repo contains the description of the second homework that uses this implementation of a log file generator in Scala. Students should clone this repo using the command ```git clone git@github.com:0x1DOCD00D/LogFileGenerator.git```. Students should invest some time to learn the implementation details of the log generator, specifically, how no ```var``` is used and how mutation is avoided and recursion is used, however, it is not required for completing this homework.

## Preliminaries
As part of the previous homework assignment you learned to create and manage your Git repository, create your application in Scala, create tests using widely popular Scalatest framework, and expande on the provided SBT build and run script for your application. Doing this homework is essential for successful completion of the rest of this course, since all other homeworks and the course project will depend on this homework: create randomly generated log files that represent big data, parse and analyze them, change their content and structure and process them in the cloud using big data analysis frameworks.

First things first, if you haven't done so, you must create your account at either [BitBucket](https://bitbucket.org/) or [Github](https://github.com/), which are Git repo management systems. Please make sure that you write your name in your README.md in your repo as it is specified on the class roster. Since it is a large class, please use your UIC email address for communications and avoid emails from other accounts like funnybunny2000@gmail.com. If you don't receive a response within 12 hours, please contact your TA or me, it may be a case that your direct emails went to the spam folder.

Next, if you haven't done so, you will install [IntelliJ](https://www.jetbrains.com/student/) with your academic license, the JDK, the Scala runtime and the IntelliJ Scala plugin and the [Simple Build Toolkit (SBT)](https://www.scala-sbt.org/1.x/docs/index.html) and make sure that you can create, compile, and run Java and Scala programs. Please make sure that you can run [various Java tools from your chosen JDK between versions 8 and 16](https://docs.oracle.com/en/java/javase/index.html).

In this and all consecutive homeworks and in the course project you will use logging and configuration management frameworks. You will comment your code extensively and supply logging statements at different logging levels (e.g., TRACE, INFO, WARN, ERROR) to record information at some salient points in the executions of your programs. All input and configuration variables must be supplied through configuration files -- hardcoding these values in the source code is prohibited and will be punished by taking a large percentage of points from your total grade! You are expected to use [Logback](https://logback.qos.ch/) and [SLFL4J](https://www.slf4j.org/) for logging and [Typesafe Conguration Library](https://github.com/lightbend/config) for managing configuration files. These and other libraries should be imported into your project using your script [build.sbt](https://www.scala-sbt.org). These libraries and frameworks are widely used in the industry, so learning them is the time well spent to improve your resumes. Also, please set up your account with [AWS Educate](https://aws.amazon.com/education/awseducate/). Using your UIC email address will enable you to receive free credits for running your jobs in the cloud. Preferably, you should create your developer account for $30 per month to enjoy the full range of AWS services.


From the implementation of the log generator you can see how to use Scala to create a fully pure functional (not imperative) implementation. As you see from the StackOverflow survey, knowledge of Scala is highly paid and in great demand, and it is expected that you pick it relatively fast, especially since it is tightly integrated with Java. I recommend using the book on Programming in Scala Fourth and Fifth Editions by Martin Odersky et al. You can obtain this book using the academic subscription on Safari Books Online. There are many other books and resources available on the Internet to learn Scala. Those who know more about functional programming can use the book on Functional Programming in Scala published on Sep 14, 2014 by Paul Chiusano and Runar Bjarnason.

When creating your Map/Reduce or CORBA program code in Scala, you should avoid using **var**s and while/for loops that iterate over collections using [induction variables](https://en.wikipedia.org/wiki/Induction_variable). Instead, you should learn to use collection methods **map**, **flatMap**, **foreach**, **filter** and many others with lambda functions, which make your code linear and easy to understand. Also, avoid mutable variables that expose the internal states of your modules at all cost. Points will be deducted for having unreasonable **var**s and inductive variable loops without explanation why mutation is needed in your code unless it is confined to method scopes - you can always do without it.

## Overview
In this homework, you will create a distributed program for parallel processing of the log files that are generated using this project that you cloned. Once you cd into the cloned project directory you can build using ```sbt clean compile``` then run tests with ```sbt test``` and then run the project with ```sbt run```. Currently, the settings in ```application.conf``` allow you to generate a random log file with 100 entries that will be put in a directory named ***log*** under the root project directory. Alternatively, you can import this project into IntelliJ and run it from within the IDE. 

Each entry in the log dataset describes a fictitios log message, which contains the time of the entry, the logging context name, the message level (i.e., INFO, WARN, DEBUG or ERROR), the name of the logging module and the message itself. The size of the log file can be controlled by setting the maximum number of log messages or the duration of the log generator run in ```application.conf```. Students can experiment with smaller log files when debugging their programs, but they should create large enough log files for this homework assignment. Each log entry is independent from the other one in that it can be processed without synchronizing with processing some other entries.

Consider the following entries in the dataset. The first entry is generated at time 09:58:55.569 followed by the second entry generated at time 09:58:55.881. The sixth entry is of type ERROR that has a smaller likelihood range in ```application.conf```. Depending on the value of the configuration parameter ```Frequency``` the regular expression specified in configuration parameter ```Pattern``` is used to create instances of this pattern which are inserted into the generated log messages. It is imperative that students read comments in this configuration file and experiment with different configuration parameter settings to see how the content of the generated log file changes.   
```
09:58:55.569 [main] INFO  GenerateLogData$ - Log data generator started...
09:58:55.881 [scala-execution-context-global-17] INFO  GenerateLogData$ - NL8Q%rvl,RBHq@|XR2U&k>"SXwcyB#iv
09:58:55.928 [scala-execution-context-global-17] WARN  Generation.Parameters$ - =5$YcP!s@h
09:59:30.849 [scala-execution-context-global-17] INFO  Generation.Parameters$ - V<Z~#Ws"WNJ:[d?+dRpaIFp23"1_oKn;Qd,>
09:59:30.867 [scala-execution-context-global-17] INFO  Generation.Parameters$ - 3FNgL<)k7+c+8yQ"3m*e#!)HK[['z+-an/Uw?J'|[<w&kbtM
09:59:30.876 [scala-execution-context-global-17] ERROR  Generation.Parameters$ - +5l}CAK:}q])
09:59:30.891 [scala-execution-context-global-17] INFO  Generation.Parameters$ - Mv8)!{uuaD3%<m.VO/[pfHLS&eIBmKx~(6
```

Your job is to determine the distribution of the number of messages that match the pattern specified in configuration parameter ```Pattern``` across many generated log files and log message types and time intervals. Paritioning this log message dataset into shards is easy, since it requires to specify the number of messages in each shard. Most likely, you will tweak settings in ```logback.xml``` to partition the dataset into an approximately equal sizes of the log file  shards.

As before, this homework script is written using a retroscripting technique, in which the homework outlines are generally and loosely drawn, and the individual students improvise to create the implementation that fits their refined objectives. In doing so, students are expected to stay within the basic requirements of the homework and they are free to experiments. Asking questions is important, so please ask away at MS Team!

## Functionality
Your homework assignment is to create a program for parallel distributed processing of the publication dataset. Your goal is to produce the following statistics about the generated log files. First, you will compute a spreadsheet or an CSV file that shows the distribution of different types of messages across predefined time intervals and injected string instances of the designated regex pattern for these log message types. Second, you will compute time intervals sorted in the descending order that contained most log messages of the type ERROR with injected regex pattern string instances. Please note that it is ok to detect instances of the designated regex pattern that were not specifically injected in log messages because they can also be randomly generated. Then, for each message type you will produce the number of the generated log messages. Finally, you will produce the number of characters in each log message for each log message type that contain the highest number of characters in the detected instances of the designated regex pattern.

### Assignment for the main textbook group
Your job is to create the mapper and the reducer for each task, explain how they work, and then to implement them and run on the log dataset that you will generate using your predefined configuration parameters. The output of your map/reduce is a spreadsheet or an CSV file with the required statistics. The explanation of the map/reduce model is given in the main textbook in Chapter 4.

You will create and run your software application using [Apache Hadoop](http://hadoop.apache.org/), a framework for distributed processing of large data sets across multiple computers (or even on a single node) using the map/reduce model. Even though you can install and configure Hadoop on your computers, I recommend that you use a virtual machine (VM) of [Hortonworks Sandbox](https://www.cloudera.com/tutorials/getting-started-with-hdp-sandbox.html), a preconfigured Apache Hadoop installation with a comprehensive software stack. To run the VM, you can install vmWare or VirtualBox. As UIC students, you have access to free vmWare licenses, go to http://go.uic.edu/csvmware to obtain your free license. In some cases, I may have to provide your email addresses to a department administrator to enable your free VM academic licenses. Please notify me if you cannot register and gain access to the webstore.

The steps for obtaining your free academic vmWare licenses are the following:
- Contact [Mr.Phil Bertran](pbeltr1@uic.edu) and CC to [DrMark](drmark@uic.edu) to obtain access to the vmWare academic program.
- One approved, go to [Onthehub vmWare](http://go.uic.edu/csvmware).
- Click on the "sign in" link at the top.
- Click on "register".
- Select "An account has been created..." and continue with the registration.
- Make sure that you use the UIC email with which you are registered with the system.

Only UIC students who are registered for this course and use the textbook option 1 are eligible. If you are auditing the course, you need to contact the uic webstore directly. Alternatively, you can use [VirtualBox from Oracle Corp](https://www.virtualbox.org/).

Next, after creating and testing your map/reduce program locally, you will deploy it and run it on the Amazon Elastic MapReduce (EMR) - you can find plenty of [documentation online](https://aws.amazon.com/emr). You will produce a short movie that documents all steps of the deployment and execution of your program with your narration and you will upload this movie to [youtube](www.youtube.com) and you will submit a link to your movie as part of your submission in the README.md file. To produce a movie, you may use an academic version of [Camtasia](https://www.techsmith.com/video-editor.html) or some other cheap/free screen capture technology from the UIC webstore or an application for a movie capture of your choice. The captured web browser content should show your login name in the upper right corner of the AWS application and you should introduce yourself in the beginning of the movie speaking into the camera.

### Assignment for the alternative textbook group
Your job is to create the distributed objects using [omniOrb CORBA framework](http://omniorb.sourceforge.net/omni42/omniORB/) for each task, explain how they work, and then to implement them and run on the generated log message dataset. The output of your distributed system is a spreadsheet or an CSV file with the required statistics. The explanation of the CORBA is given in the alternative textbook in Chapter 7 -Guide to Reliable Distributed Systems: Building High-Assurance Applications and Cloud-Hosted Services 2012th Edition by Kenneth P. Birman. You can complete your implementation using C++ or Python.

Next, after creating and testing your program locally, you will deploy it and run it on the AWS EC2 IaaS. You will produce a short movie that documents all steps of the deployment and execution of your program with your narration and you will upload this movie to [youtube](www.youtube.com) and you will submit a link to your movie as part of your submission in the README.md file. To produce a movie, you may use an academic version of [Camtasia](https://www.techsmith.com/video-editor.html) or some other cheap/free screen capture technology from the UIC webstore or an application for a movie capture of your choice. The captured web browser content should show your login name in the upper right corner of the AWS application and you should introduce yourself in the beginning of the movie speaking into the camera.

## Baseline Submission
Your baseline project submission should include your implementation, a conceptual explanation in the document or in the comments in the source code of how your mapper and reducer work to solve the problem, and the documentation that describe the build and runtime process, to be considered for grading. Your project submission should include all your source code as well as non-code artifacts (e.g., configuration files), your project should be buildable using the SBT, and your documentation must specify how you paritioned the data and what input/outputs are.

## Collaboration
You can post questions and replies, statements, comments, discussion, etc. on Teams using the corresponding channel. For this homework, feel free to share your ideas, mistakes, code fragments, commands from scripts, and some of your technical solutions with the rest of the class, and you can ask and advise others using Teams on where resources and sample programs can be found on the Internet, how to resolve dependencies and configuration issues. When posting question and answers on Tea,s, please make sure that you selected the appropriate channel, to ensure that all discussion threads can be easily located. Active participants and problem solvers will receive bonuses from the big brother :-) who is watching your exchanges (i.e., your class instructor and your TA). However, *you must not describe your simulation or specific details related how your construct your models!*

## Git logistics
**This is an individual homework.** If you read this description it means that you located the [Github repo for this homework](https://github.com/0x1DOCD00D/LogFileGenerator). Please remember to grant a read access to your repository to your TA and your instructor. In future, for the team homeworks and the course project, you should grant the write access to your forkmates, but NOT for this homework. You can commit and push your code as many times as you want. Your code will not be visible and it should not be visible to other students (except for your forkmates for a team project, but not for this homework). Announcing a link to your public repo for this homework or inviting other students to join your fork for an individual homework before the submission deadline will result in losing your grade. For grading, only the latest commit timed before the deadline will be considered. **If your first commit will be pushed after the deadline, your grade for the homework will be zero**. For those of you who struggle with the Git, I recommend a book by Ryan Hodson on Ry's Git Tutorial. The other book called Pro Git is written by Scott Chacon and Ben Straub and published by Apress and it is [freely available](https://git-scm.com/book/en/v2/). There are multiple videos on youtube that go into details of the Git organization and use.

Please follow this naming convention to designate your authorship while submitting your work in README.md: "Firstname Lastname" without quotes, where you specify your first and last names **exactly as you are registered with the University system**, so that we can easily recognize your submission. I repeat, make sure that you will give both your TA and the course instructor the read/write access to your *private forked repository* so that we can leave the file feedback.txt with the explanation of the grade assigned to your homework.

## Discussions and submission
As it is mentioned above, you can post questions and replies, statements, comments, discussion, etc. on Teams. Remember that you cannot share your code and your solutions privately, but you can ask and advise others using Teams and StackOverflow or some other developer networks where resources and sample programs can be found on the Internet, how to resolve dependencies and configuration issues. Yet, your implementation should be your own and you cannot share it. Alternatively, you cannot copy and paste someone else's implementation and put your name on it. Your submissions will be checked for plagiarism. **Copying code from your classmates or from some sites on the Internet will result in severe academic penalties up to the termination of your enrollment in the University**.


## Submission deadline and logistics
Sunday, October 17, 2021 at 10PM CST via email to the instructor and your TA that lists your name and the link to your repository. Your submission repo will include the code for the program, your documentation with instructions and detailed explanations on how to assemble and deploy your program along with the results of your program execution, the link to the video and a document that explains these results based on the characteristics and the configuration parameters of your log generator, and what the limitations of your implementation are. Again, do not forget, please make sure that you will give both your TAs and your instructor the read access to your private repository. Your code should compile and run from the command line using the commands **sbt clean compile test** and **sbt clean compile run**. Also, you project should be IntelliJ friendly, i.e., your graders should be able to import your code into IntelliJ and run from there. Use .gitignore to exlude files that should not be pushed into the repo.


## Evaluation criteria
- the maximum grade for this homework is 9%. Points are subtracted from this maximum grade: for example, saying that 2% is lost if some requirement is not completed means that the resulting grade will be 9%-2% => 6%; if the core homework functionality does not work or it is not implemented as specified in your documentation, your grade will be zero;
- only some basic map/reduce or CORBA examples from some repos are given and nothing else is done: zero grade;
- having less than five unit and/or integration scalatests: up to 5% lost;
- missing comments and explanations from your program: up to 5% lost;
- logging is not used in your programs: up to 3% lost;
- hardcoding the input values in the source code instead of using the suggested configuration libraries: up to 4% lost;
- for each used *var* for heap-based shared variables or mutable collections: 0.2% lost;
- for each used *while* or *for* or other loops with induction variables to iterate over a collection: 0.2% lost;
- no instructions in README.md on how to install and run your program: up to 5% lost;
- the program crashes without completing the core functionality: up to 6% lost;
- the documentation exists but it is insufficient to understand your program design and models and how you assembled and deployed all components of the cloud: up to 5% lost;
- the minimum grade for this homework cannot be less than zero.

That's it, folks!