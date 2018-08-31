# Kind Sir

Kind Sir is a [Gitlab](https://gitlab.com/) satellite project.
It allows to automatically accept merge requests which got
specific number of upvotes during the code review with respect to build status.

# How it works
Application reads config file and grabs a list of groups from Gitlab API to monitor.
The separate supervisor actor will be spawned for each group.
Once per minute groups' supervisor spawns Repo Worker actor for each repository in
this group.

Repo worker tries to find `.kind_sir.conf` file in the root of repository.
If there is no such file, worker will stop and repository will not
be processed. Otherwise Repo Worker will read this file and grab a list of
opened Merge Requests from the repository. After that it will check the number of
upvotes and downvotes for every merge request. And if request has
enought upvotes and meets the requirements of Veto settings and build status,
Repo Worker will try to merge it.

## Installation

- Git clone this repository
- Run `activator assembly` inside the repository directory
- Grab `kind_sir.jar` file produced by the `assembly` from the root of project directory.
- Deploy this file to the target system
- Create Gitlab user on behalf of whom Kind Sir will be acting
- Add this user to any number of groups, which you want to be monitored by Kind Sir
- Find `private_token` of this user in Gitlab's profile section
- Create configuration file described below
- Run `java -Dconfig.file=<your-config.conf> <kind-sir-assembly.jar>`

## Run in docker

- `git clone https://github.com/answr42/kind-sir.git`
- `cd kind-sir`
- `touch config.conf` and [fill it](#configuration)
- `docker build -t kind-sir .`
- `docker run --name kind-sir -d kind-sir`

## Configuration

Kind Sir uses two kinds of configuration files:

1. Application configuration file, deployed with Kind Sir itself
2. `kind_sir.conf`. Acceptance policy configuration placed in the root
of monitored repository

### Application configuration file format

App configuration file is just a normal [Lightbend Config](https://github.com/typesafehub/config/) file.
There are only two fields you can specify here:

1. Gitlab URL
2. Private Token for a user on whose behalf Kind Sir will be
acting.
3. Gitlab API version. If you don't know your GitLab API version, you can check GitLab version at [https://your.gitlab.domain.name/help] and than choose:

* If GitLab < 9.0, you probably need to choose **3**.
* If GitLab >= 9.0, you still can use **3**, but **4** is the preferred version to be used. 
* If GitLab >= 9.5, **4** is only available version at that moment.

For example, let's imagine you created `config.conf`:

```
kindSir {
  gitlab-url = "https://gitlab.acme.com"
  gitlab-token = "ex_MT1jP5t1DeSnN4ka9"
  gitlab-api-version = 4
}
```

After that you can run Kind Sir with command:
```
java -Dconfig.file=config.conf -jar kind-sir-assembly-0.1.jar
```

### Acceptance policy

You should place config file named `.kind_sir.conf` in the root of
every repository you want to be monitored by Kind Sir.
This is a regular JSON file.

You can find an example [right in this repository](.kind_sir.conf).

There are also only three things to specify:

1. `upvotes_threshold`: if the number of upvotes minus number of downvotes exceed specified number,
request will be merged
2. `veto_enabled`: true value means request will never be accepted if
there is at least one downvote.
3. Optional `ignore_build_status`: if true, Kind Sir will merge request even if build failed or
there were no build at all.

Example:
```
{
  "upvotes_threshold": 2,
  "veto_enabled": false,
  "ignore_build_status": false
}
```
