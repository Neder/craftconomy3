echo MD5:
md5sum ./craftconomy_${TRAVIS_JOB_NUMBER}.tar.gz
echo Uploading craftconomy_${TRAVIS_JOB_NUMBER}.tar.gz
sudo curl -T ./craftconomy_${TRAVIS_JOB_NUMBER}.tar.gz -u u262377766.towny:${PASSWORD} ftp://ftp.ocw5902.esy.es/BuildTools/
