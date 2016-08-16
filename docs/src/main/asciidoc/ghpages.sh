#!/bin/bash -x

git remote set-url --push origin `git config remote.origin.url | sed -e 's/^git:/https:/'`

if ! (git remote set-branches --add origin gh-pages && git fetch -q); then
    echo "No gh-pages, so not syncing"
    exit 0
fi

if ! [ -d docs/target/generated-docs ]; then
    echo "No gh-pages sources in docs/target/generated-docs, so not syncing"
    exit 0
fi

# The script should be executed from the root folder

ROOT_FOLDER=`pwd`
echo "Current folder is ${ROOT_FOLDER}"

if [[ ! -e "${ROOT_FOLDER}/.git" ]]; then
    echo "You're not in the root folder of the project!"
    exit 1
fi

# Retrieve properties
###################################################################

# Prop that will let commit the changes
COMMIT_CHANGES="no"

# Get the name of the `docs.main` property
MAIN_ADOC_VALUE=$(mvn -q \
    -Dexec.executable="echo" \
    -Dexec.args='${docs.main}' \
    --non-recursive \
    org.codehaus.mojo:exec-maven-plugin:1.3.1:exec)
echo "Extracted 'main.adoc' from Maven build [${MAIN_ADOC_VALUE}]"

# Get whitelisted branches - assumes that a `docs` module is available under `docs` profile
WHITELIST_PROPERTY="docs.whitelisted.branches"
WHITELISTED_BRANCHES_VALUE=$(mvn -q \
    -Dexec.executable="echo" \
    -Dexec.args="\${${WHITELIST_PROPERTY}}" \
    org.codehaus.mojo:exec-maven-plugin:1.3.1:exec \
    -P docs \
    -pl docs)
echo "Extracted '${WHITELIST_PROPERTY}' from Maven build [${WHITELISTED_BRANCHES_VALUE}]"

# Code getting the name of the current branch. For master we want to publish as we did until now
# http://stackoverflow.com/questions/1593051/how-to-programmatically-determine-the-current-checked-out-git-branch
CURRENT_BRANCH=$(git symbolic-ref -q HEAD)
CURRENT_BRANCH=${CURRENT_BRANCH##refs/heads/}
CURRENT_BRANCH=${CURRENT_BRANCH:-HEAD}
echo "Current branch is [${CURRENT_BRANCH}]"

# Stash any outstanding changes
###################################################################
git diff-index --quiet HEAD
dirty=$?
if [ "$dirty" != "0" ]; then git stash; fi

# Switch to gh-pages branch to sync it with master
###################################################################
git checkout gh-pages
git pull origin gh-pages

# Add git branches
###################################################################
mkdir -p ${ROOT_FOLDER}/${CURRENT_BRANCH}
if [[ "${CURRENT_BRANCH}" == "master" ]] ; then
    echo -e "Current branch is master - will copy the current docs only to the root folder"
    for f in docs/target/generated-docs/*; do
        file=${f#docs/target/generated-docs/*}
        if ! git ls-files -i -o --exclude-standard --directory | grep -q ^$file$; then
            # Not ignored...
            cp -rf $f ${ROOT_FOLDER}/
            git add -A ${ROOT_FOLDER}/$file
        fi
    done
    COMMIT_CHANGES="yes"
else
    echo -e "Current branch is [${CURRENT_BRANCH}]"
    # http://stackoverflow.com/questions/29300806/a-bash-script-to-check-if-a-string-is-present-in-a-comma-separated-list-of-strin
    if [[ ",${WHITELISTED_BRANCHES_VALUE}," = *",${CURRENT_BRANCH},"* ]] ; then
        echo -e "Branch [${CURRENT_BRANCH}] is whitelisted! Will copy the current docs to the [${CURRENT_BRANCH}] folder"
        for f in docs/target/generated-docs/*; do
            file=${f#docs/target/generated-docs/*}
            if ! git ls-files -i -o --exclude-standard --directory | grep -q ^$file$; then
                # Not ignored...
                # We want users to access 1.0.0.RELEASE/ instead of 1.0.0.RELEASE/spring-cloud.sleuth.html
                if [[ "${file}" == "${MAIN_ADOC_VALUE}.html" ]] ; then
                    # We don't want to copy the spring-cloud-sleuth.html
                    # we want it to be converted to index.html
                    cp -rf $f ${ROOT_FOLDER}/${CURRENT_BRANCH}/index.html
                    git add -A ${ROOT_FOLDER}/${CURRENT_BRANCH}/index.html
                else
                    cp -rf $f ${ROOT_FOLDER}/${CURRENT_BRANCH}
                    git add -A ${ROOT_FOLDER}/${CURRENT_BRANCH}/$file
                fi
            fi
        done
        COMMIT_CHANGES="yes"
    else
        echo -e "Branch [${CURRENT_BRANCH}] is not on the white list! Check out the Maven [${WHITELIST_PROPERTY}] property in
         [docs] module available under [docs] profile. Won't commit any changes to gh-pages for this branch."
    fi
fi

if [[ "${COMMIT_CHANGES}" == "yes" ]] ; then
    git commit -a -m "Sync docs from ${CURRENT_BRANCH} to gh-pages"

    # Uncomment the following push if you want to auto push to
    # the gh-pages branch whenever you commit to master locally.
    # This is a little extreme. Use with care!
    ###################################################################
    git push origin gh-pages
fi

# Finally, switch back to the master branch and exit block
git checkout ${CURRENT_BRANCH}
if [ "$dirty" != "0" ]; then git stash pop; fi

exit 0