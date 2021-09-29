# LSDiff: Logical Structural Diff
Discovering and Representing Systematic Code Changes

[Website for LSDiff Tool Demo](http://web.cs.ucla.edu/~miryung/lsdiff-web/index.html)

## Summary of LSDiff
Software engineers often inspect program differences when reviewing others’ code changes, when writing check-in comments, or when determining why a program behaves differently from expected behavior. Program differencing tools that support these tasks are limited in their ability to group related code changes or to detect potential inconsistency in program changes. To overcome these limitations and to complement existing approaches, we built Logical Structural Diff (LSDiff) that infers systematic structural differences as logic rules, noting anomalies from systematic changes as exceptions to the logic rules. We conducted a focus group study with professional software engineers in a large E-commerce company and also compared LSDiff’s results with plain structural differences without rules and textual differences. Our evaluation suggests that LSDiff complements existing differencing tools by grouping code changes that form systematic change patterns regardless of their distribution throughout the code and that its ability to discover anomalies shows promise in detecting inconsistent changes.

## Team 
This project is developed by Professor [Miryung Kim](http://web.cs.ucla.edu/~miryung/)'s Software Engineering and Analysis Laboratory at UCLA. 
If you encounter any problems, please open an issue or feel free to contact us:

[Miryung Kim](http://web.cs.ucla.edu/~miryung/): Professor at UCLA, miryung@cs.ucla.edu;
## How to cite

If you reuse the code, please cite the paper:
```
@INPROCEEDINGS{5070531,
  author={Kim, Miryung and Notkin, David},
  booktitle={2009 IEEE 31st International Conference on Software Engineering}, 
  title={Discovering and representing systematic code changes}, 
  year={2009},
  volume={},
  number={},
  pages={309-319},
  doi={10.1109/ICSE.2009.5070531}}
```
Research paper: [Discovering and Representing Systematic Code Changes, Miryung Kim and David Notkin, ICSE '09](http://web.cs.ucla.edu/~miryung/Publications/icse09-lsdiff.pdf)

Tool Demo: [LSdiff: A Program Differencing Tool to Identify Systematic Structural Differences, Alex Loh and Miryung Kim, ICSE '10](http://web.cs.ucla.edu/~miryung/Publications/icse10-lsdifftool.pdf)
