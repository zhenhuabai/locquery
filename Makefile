OUTDIR = out
VERTXDIR = ../vertx_template
FATDIR = target
FATVER = $(shell ls target/*-fat.jar |cut -d\- -f2)
TEMPLATEDIR = data
SCRIPTSDIR = scripts
RELEASEDIR = $(OUTDIR)/release
SVNDIR = $(OUTDIR)/locquery
SVNSRCDIR = $(SVNDIR)/project
SVNRELDIR = $(SVNDIR)/release
RELEASE = $(FATDIR)/*-fat.jar

$(RELEASE): build releasedir copyfiles
package: notestbuild releasedir copyfiles
.PHONY: $(RELEASE)
notestbuild:
	mvn clean package -Dmaven.test.skip=true
build:
	mvn clean package
releasedir:
	rm -rf $(RELEASEDIR)
	mkdir -p $(RELEASEDIR)
	mkdir $(RELEASEDIR)/lib
copyfiles:
	cp -rf $(VERTXDIR)/vertx $(RELEASEDIR)/lib
	cp $(FATDIR)/*-fat.jar $(RELEASEDIR)/lib
	cp $(TEMPLATEDIR)/log4j2.xml $(RELEASEDIR)/lib
	cp $(TEMPLATEDIR)/supportedcities.csv $(RELEASEDIR)/lib
	cp $(TEMPLATEDIR)/config.json $(RELEASEDIR)
	cp $(TEMPLATEDIR)/chinese.csv $(RELEASEDIR)
	cp $(SCRIPTSDIR)/startsvr $(RELEASEDIR)
	cp $(SCRIPTSDIR)/stopsvr $(RELEASEDIR)
archieve:
	rm -rf $(OUTDIR)/locquery_r_*.tar.gz
	cd $(RELEASEDIR);tar cfz ../locquery_r_$(FATVER).tar.gz *
svnrelease: svndir released
	cp -rf src $(SVNSRCDIR)
	cp $(TEMPLATEDIR)/chinese.csv $(SVNSRCDIR)
	cp $(TEMPLATEDIR)/config.json $(SVNSRCDIR)
	cp log4j2.xml $(SVNSRCDIR)
	cp pom.xml $(SVNSRCDIR)
	cp $(SCRIPTSDIR)/startsvr $(SVNSRCDIR)
	cp $(SCRIPTSDIR)/stopsvr $(SVNSRCDIR)
	cp $(TEMPLATEDIR)/supportedcities.csv $(SVNSRCDIR)
svndir:
	rm -rf $(SVNDIR)
	mkdir -p $(SVNSRCDIR)
	mkdir -p $(SVNRELDIR)
released:
	cp -rf $(OUTDIR)/locquery_r_*.tar.gz  $(SVNRELDIR)
	cp -rf $(TEMPLATEDIR)/readme.txt  $(SVNRELDIR)/readme_r_$(FATVER).txt
clean:
	rm -rf $(RELEASEDIR);rm -rf $(SVNDIR)
