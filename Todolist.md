### remain works of implementation

- [X] implement a demo Java wrapper for LiteIndex
- [ ] provide query apis like sql for users
- [ ] fully implement the Index table on disk with sqlite and restore the indexes
- [ ] store the json records using sqlite and manipulate with dids



### writing works for journal
- [ ] append half page introduction
- [ ] append half page background? (background about keyword extraction?)
- [ ] list out the apis in chart
- [ ] append one page technique
- [X] algorithm of keyword extraction
- [X] alogrithm of pruning policy
- [ ] append three pages of evaluation and experiment (redo all?)
- [ ] one page discussion 
- [ ] related work



### advice from the review report of ASPDAC

#### Experiment
- [ ] the experiment should be on the real device instead of emulator. Yet mobile phones published current years are prefered. (expermient setup)
- [ ] The choosing of the parameters in the experiment needs justification, such as why 500 n 1000? (dataset)
- [ ] The test dataset of the experiment should be explained and maybe choose more kinds of dataset (datasets)
- [ ] A comparison with NoSQL mobile databases, such as CouchBase Lite (Using other databases)
- [ ] CPU and memory overhead with data support (Hardware Test)

#### Technique
- [ ] Clearer explaination of the trade off between wordvec and TF-IDF (Keyword Extraction)
- [ ] More explaination about the hash collision and rehashing (LiteIndex)
- [ ] The parameters of the Q-learning part should be defined earlier (Pruning Policy)
- [ ] Explain the threshold of pruning policy (Pruning Policy)
- [ ] Explain more about what if the queried index has been pruned (Pruning Policy)
