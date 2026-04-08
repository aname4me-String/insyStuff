FROM postgres:13
RUN localedef -i de_AT -c -f UTF-8 -A /usr/share/locale/locale.alias de_AT.UTF-8
ENV LANG=de_AT.utf8
