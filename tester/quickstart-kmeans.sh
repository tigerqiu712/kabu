#/**
# * Licensed to the Apache Software Foundation (ASF) under one or more
# * contributor license agreements.  See the NOTICE file distributed with
# * this work for additional information regarding copyright ownership.
# * The ASF licenses this file to You under the Apache License, Version 2.0
# * (the "License"); you may not use this file except in compliance with
# * the License.  You may obtain a copy of the License at
# *
# *     http://www.apache.org/licenses/LICENSE-2.0
# *
# * Unless required by applicable law or agreed to in writing, software
# * distributed under the License is distributed on an "AS IS" BASIS,
# * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# * See the License for the specific language governing permissions and
# * limitations under the License.
# */

#
# Downloads the Reuters dataset and prepares it for clustering
#
# To run:  change into the mahout directory and type:
#  examples/bin/build-reuters.sh
#!/bin/sh

#cd examples/bin/
#mkdir -p work
#if [ ! -e work/reuters-out ]; then
#  if [ ! -e work/reuters-sgm ]; then
#    if [ ! -f work/reuters21578.tar.gz ]; then
#      echo "Downloading Reuters-21578"
#      curl http://kdd.ics.uci.edu/databases/reuters21578/reuters21578.tar.gz  -o work/reuters21578.tar.gz
#    fi
#    mkdir -p work/reuters-sgm
#    echo "Extracting..."
#    cd work/reuters-sgm && tar xzf ../reuters21578.tar.gz && cd .. && cd ..
#  fi
#fi

#cd ../..
/Users/mike/Documents/mahout-distribution-0.7/bin/mahout org.apache.lucene.benchmark.utils.ExtractReuters ./kmeans/input/reuters/ ./kmeans/input/reuters_ready/
/Users/mike/Documents/mahout-distribution-0.7/bin/mahout seqdirectory -i ./kmeans/input/reuters_ready/ -o ./kmeans/reuters-seqfiles/ -c UTF-8 -chunk 5
/Users/mike/Documents/mahout-distribution-0.7/bin/mahout seq2sparse -i ./kmeans/reuters-seqfiles/ -o ./kmeans/newClusters/
/Users/mike/Documents/mahout-distribution-0.7/bin/mahout kmeans -i ./kmeans/newClusters/tfidf-vectors/ -c ./kmeans/newClusters/centroids -o ./kmeans/newClusters/clusters -x 10 -k 20 -ow
/Users/mike/Documents/mahout-distribution-0.7/bin/mahout clusterdump --input ./kmeans/newClusters/clusters/clusters-10-final --output ./kmeans/output/output_final -d ./kmeans/newClusters/dictionary.file-0 -dt sequencefile -b 100 -n 20

