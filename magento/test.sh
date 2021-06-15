#!/bin/bash

find /var/www/versions/$1 -type f -exec chmod 644 {} \;
find /var/www/versions/$1 -type d -exec chmod 755 {} \;
chown -R www-data:www-data /var/www/versions/$1
chmod u+x /var/www/versions/$1/bin/magento


#
