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

# Find name of current branch
###################################################################
branch=$TRAVIS_BRANCH
[ "$branch" == "" ] && branch=`git rev-parse --abbrev-ref HEAD`
target=.
if [ "$branch" != "master" ]; then target=./$branch; mkdir -p $target; fi

# Stash any outstanding changes
###################################################################
git diff-index --quiet HEAD
dirty=$?
if [ "$dirty" != "0" ]; then git stash; fi

# Switch to gh-pages branch to sync it with current branch
###################################################################
git checkout gh-pages

for f in docs/target/generated-docs/*; do
    file=${f#docs/target/generated-docs/*}
    if ! git ls-files -i -o --exclude-standard --directory | grep -q ^$file$; then
        # Not ignored...
        cp -rf $f $target
        git add -A $target/$file
    fi
done

git add -A README.adoc || echo "No change to README.adoc"
git commit -a -m "Sync docs from $branch to gh-pages" || echo "Nothing committed"

# Uncomment the following push if you want to auto push to
# the gh-pages branch whenever you commit to branch locally.
# This is a little extreme. Use with care!
###################################################################
git push origin gh-pages || echo "Cannot push gh-pages"

# Finally, switch back to the current branch and exit block
git checkout $branch
if [ "$dirty" != "0" ]; then git stash pop; fi

exit 0
