# project-dist-cal
Distributed systems project 1

This project is to implement a distributed calendar application using a replicated log and dictionary.  To share calendar information the Wuu and Bernstein algorithm was used (Efficient solutions to the replicated log and dictionary problems, G. Wuu and A. Bernstein, Proceedings of the third annual ACM symposium on Principles of Distributed Computing, 1984).  

We had to run the application on 4 instances in different regions on Amazon EC2.  Each node needed to be able to recover from crash failures and a conflict resolution protocol needed to be implemented to ensure the calendars stay consistent.  
