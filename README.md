# networkdiscoveryclient

This is a 'proof-of-concept' I made in order to start up multiple docker instances with RethinkDB where they discover who's the master and who's the slave, and then join the master etc.


It is in no way finished, but it's somewhere to start.

It uses UDP to broadcast which docker instances are there, and if a docker instance stops broadcasting it's removed from the available servers.
