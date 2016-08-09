#!/bin/bash


function usage(){
  echo $1
  echo "Usage: $0 -b s3 bucket to upload
                   -d folder in bucket to save file
                   -f local file path to upload
                   -m s3 restful method PUT|GET|DELETE
                   -H optional s3 host name, default is 's3'
                   -o optional GET file local output path
                   -h optional print usage"
  exit $2
}

if [ -z $s3Key ]; then
   usage "env variable s3Key not set" 1
fi

if [ -z $s3Secret ]; then
   usage "env variable s3Secret not set" 1
fi

if [ -z $1 ]; then
   usage "Please provide parameters" 1
fi

while [ "$1" != "" ]; do
    case $1 in
        -b )    shift
                bucket=$1
                                ;;
        -f )    shift
                file=$1
                                ;;
        -o )    shift
                ofile=$1
                                ;;
        -d )    shift
                path=$1
                                ;;
        -m )    shift
                op=$1
                                ;;
        -d )    shift
                path=$1
                                ;;
        -H )    shift
                s3=$1
                                ;;
        -h )    usage "" 0
                                ;;
        * )     usage "unknown parameters" 1

    esac
    shift
done

if [ -z $bucket ]; then
   usage "No bucket -b provided" 1
fi

if [ -z $op ]; then
   usage "No s3 operation method -m provided" 1
fi

if [ -z $s3 ]; then
   s3="s3"
fi

resource="/$bucket/$path/$file"

contentType=""
dateValue=`date -R`
stringToSign="$op\n\n$contentType\n$dateValue\n$resource"

if [ $op = 'PUT' ]; then
    if [ ! -s $file ]; then
      usage "$file not found" 1
    fi
    op="-X $op -T $file "
elif [ $op = 'GET' ]; then
      if [ -z $ofile ]; then
        ofile=$file
      fi
      op="-o $ofile"
elif [ $op = 'DELETE' ]; then
   op="-X $op"
else
   usage "unknow S3 operation method $op" 1
fi


signature=`echo -en $stringToSign | openssl sha1 -hmac $s3Secret -binary | base64`
curl  $op \
  -H "Host: $bucket.$s3.amazonaws.com" \
  -H "Date: $dateValue" \
  -H "Content-Type: $contentType" \
  -H "Authorization: AWS $s3Key:$signature" \
https://$bucket.$s3.amazonaws.com/$path/$file
