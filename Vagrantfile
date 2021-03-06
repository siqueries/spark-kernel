#
# Copyright 2014 IBM Corp.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# -*- mode: ruby -*-
# vi: set ft=ruby :

$script = <<-SCRIPT
mkdir -p /var/lib/vagrant_dev_install_flags
function flag_is_set() {
  if [ -e /var/lib/vagrant_dev_install_flags/$1 ]; then
    return 0
  else
    return 1
  fi
}

function set_flag() {
  touch /var/lib/vagrant_dev_install_flags/$1
}

function unset_flag() {
  rm -f /var/lib/vagrant_dev_install_flags/$1
}

function unset_all_flags() {
  rm -f /var/lib/vagrant_dev_install_flags/*
}

# Set vagrant user pw
printf "vagrant:vagrant\n" | chpasswd

# Update before we go
apt-get update

# Install Java and other dependencies
if ! flag_is_set CORE_DEPS; then
  apt-get -y install openjdk-7-jdk maven wget build-essential git uuid-dev && \
  set_flag CORE_DEPS
fi

# Install IPython and ZeroMQ
IPYTHON_VERSION=3.0.0

if ! flag_is_set IPYTHON; then
  apt-get -f -y install && \
  apt-get -y install python-pip python-dev libzmq-dev build-essential && \
  cd /src && \
  pip install pyzmq tornado runipy jsonschema jinja2 && \
  pip install -e git+https://github.com/ipython/ipython.git@rel-${IPYTHON_VERSION}#egg=ipython
  pip install -e src/ipython[notebook]
  ipython profile create && \
  set_flag IPYTHON
fi

if [ -z `which docker` ]; then
  sudo apt-get update -y
  sudo apt-get -y install openjdk-7-jdk apt-transport-https linux-image-extra-`uname -r`
  printf "deb https://get.docker.com/ubuntu docker main\n" > /etc/apt/sources.list.d/docker.list
  sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 36A1D7869245C8950F966E92D8576A8BA88D21E9
  sudo apt-get update -y
  sudo apt-get install -y lxc-docker-1.4.1
  sudo chown vagrant:vagrant /var/run/docker.sock
fi

# Install scala and sbt (if not already installed)
cd /tmp

# If Scala is not installed, install it
if ! flag_is_set SCALA; then
  apt-get install -f -y && \
  apt-get install -y libjansi-java && \
  apt-get install -f -y && \
  wget --progress=bar:force http://www.scala-lang.org/files/archive/scala-2.10.4.deb && \
  dpkg -i scala-2.10.4.deb && \
  rm scala-2.10.4.deb && \
  set_flag SCALA
fi

# If sbt is not installed, install it
if ! flag_is_set SBT; then
  wget --progress=bar:force http://dl.bintray.com/sbt/debian/sbt-0.13.7.deb && \
  dpkg -i sbt-0.13.7.deb && \
  rm sbt-0.13.7.deb && \
  set_flag SBT
fi

# Add Spark Kernel json to IPython configuration
echo "Adding kernel.json"
mkdir -p /home/vagrant/.ipython/kernels/spark
cat << EOF > /home/vagrant/.ipython/kernels/spark/kernel.json
{
    "display_name": "Spark 1.1.0 (Scala 2.10.4)",
    "language_info": { "name": "scala" },
    "argv": [
        "/home/vagrant/local/bin/sparkkernel",
        "--profile",
        "{connection_file}"
    ],
    "codemirror_mode": "scala"
}
EOF

# Add Scala syntax highlighting support to custom.js of default profile
printf "Appending to profile_default custom.js\n"
(su vagrant
mkdir -p /home/vagrant/.ipython/profile_default/static/custom/
cat << EOF >> /home/vagrant/.ipython/profile_default/static/custom/custom.js
CodeMirror.requireMode('clike',function(){
    "use strict";

    CodeMirror.defineMode("scala", function(conf, parserConf) {
        var scalaConf = {};
        for (var prop in parserConf) {
            if (parserConf.hasOwnProperty(prop)) {
                scalaConf[prop] = parserConf[prop];
            }
        }
       
        scalaConf.name = 'text/x-scala';

        var mode = CodeMirror.getMode(conf, scalaConf);

        return mode;
    }, 'scala');

    CodeMirror.defineMIME("text/x-spark", "spark", "scala");
})
EOF
)

chown -R vagrant.vagrant /home/vagrant/.ipython

SCRIPT

Vagrant.configure("2") do |config|
  # Have the script install docker
  config.vm.provision :shell, :inline => $script

  # Every Vagrant virtual environment requires a box to build off of.
  config.vm.box = "trusty_ubuntu"
  config.vm.box_url = "https://cloud-images.ubuntu.com/vagrant/trusty/current/trusty-server-cloudimg-amd64-vagrant-disk1.box"
  config.vm.hostname = "host-box"

  # Mount the directory containing this file as /vagrant in the VM.
  # Since this file is copied around we need to figure out where the docker files are
  config.vm.synced_folder "./" , "/src/spark-kernel"

  # Create a private network, which allows host-only access to the machine
  # using a specific IP. Make sure this IP doesn't exist on your local network.
  config.vm.network :private_network, ip: "192.168.44.44"
  config.vm.network :forwarded_port, guest: 22, host: 2223

  # Configure memory and cpus
  config.vm.provider :virtualbox do |vb|
    vb.customize ["modifyvm", :id, "--memory", "2048"]
    vb.customize ["modifyvm", :id, "--cpus", "2"]
  end
end
