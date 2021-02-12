# db-schema-jobs

## Notes for running in Minishift

Set the following additional environment variables on the Jenkins deployment config:
```
oc set env dc/jenkins GRETL_JOB_REPO_URL_DB_SCHEMA=https://github.com/schmandr/db-schema-jobs.git
oc set env dc/jenkins GRETL_JOB_REPO_URL_DB_SCHEMA_PRIVILEGES=git@github.com:USER/REPO.git
```

Steps to perform for checking out a private GitHub repository:
* Create an SSH key (don't set a passphrase when asked for it): TODO: Check if better use a passphrase, as the key won't be encrypted... (https://cloudowski.com/articles/jenkins-on-openshift/)
```
ssh-keygen -f ~/github_deploy_key_db-schema-privileges -t ed25519 -C "GRETL"
```
* Display the public key that has been generated:
```
cat ~/github_deploy_key_db-schema-privileges.pub
```
* Log into GitHub and go to the repository; navigate to *Settings* > *Deploy keys*;
  click *Add deploy key* and set the title *GRETL*
  and paste the contents of the public key into the *Key* field; don't set *Allow write access*
* Add the private key to Minishift and have it synced into Jenkins:
```
oc create secret generic db-schema-privileges-deploy-key --from-file=ssh-privatekey=~/github_deploy_key_db-schema-privileges
oc label secret db-schema-privileges-deploy-key credential.sync.jenkins.openshift.io=true
```

Noet: If checking out using SSH doesn't work in your environment, check at https://www.openshift.com/blog/private-git-repositories-part-3-personal-access-tokens for a different solution.
