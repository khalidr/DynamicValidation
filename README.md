docker pull webpt/couchbase-server

docker run -d --name couchbase-server -p 8091-8094:8091-8094 -p 11210-11211:11210-11211 webpt/couchbase-server