# Authors:
#  Puneet Bansal
#  Sadaf H Syed

if [ "$#" -ne 2 ];
then
	echo "Usage: $0 contextWord queryWord"
	exit 1
fi
 
# start hadoop
start-dfs.sh
echo "Waiting for Hadoop to be fully online. This may take up to 30 seconds..."
sleep 25
start-mapred.sh
hadoop dfsadmin -safemode wait

hadoop dfs -rmr psanalyzer/input
hadoop dfs -rmr psanalyzer/output

# copy input files
hadoop dfs -mkdir psanalyzer/input
hadoop fs -put ./HW3_Input/* psanalyzer/input/
#hadoop dfs -ls psanalyzer/input

# remove previous output files if any
if [ -d output ]
then
	rm -rf output
fi

#hadoop dfs -rmr psanalyzer/output
# run TextAnalyzer
hadoop jar ./textanalyzer.jar TextAnalyzer $1 $2 psanalyzer/input psanalyzer/output

# get output
hadoop fs -get psanalyzer/output output

[ ! -f output/part-00000 ] && { echo "Error: output file not found."; exit 2; }

echo "**************** Output *****************"
if [ -s output/part-00000 ]
then
	output=`cat output/part-00000`
	outputValue=$(echo $output | sed 's/ /_/g')
	echo "$1 $2 $outputValue"
else
	outputValue="0"
	echo "$1 $2 $outputValue"
fi
echo "*****************************************"

# stop hadoop
stop-all.sh
