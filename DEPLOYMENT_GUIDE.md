# ================================================================
# COMPLETE DEPLOYMENT GUIDE - Shranvi Spring Boot API
# For Beginners - Every Step Explained in Detail
# ================================================================

## 🗺️ WHAT WE ARE BUILDING

Your CI/CD flow will work like this:

```
You push code to GitHub
        ↓
GitHub sends a notification to Jenkins (webhook)
        ↓
Jenkins automatically:
  1. Downloads your code
  2. Compiles it (Maven Build)
  3. Runs all unit tests (JUnit)
  4. Checks code quality (SonarQube)
  5. Scans for security issues (OWASP)
  6. Packages into JAR file
  7. Builds Docker image
  8. Pushes image to Docker Hub
  9. SSH into your EC2 server
  10. Pulls new image and restarts container on PORT 8081
  11. Runs health check to confirm it's working
```

Your EC2 server will have:
- Flask app running on port 5000 (existing)
- Spring Boot API running on port 8081 (new - no conflict!)

---

## 📋 PREREQUISITES - What You Need

- EC2 server (Ubuntu 22.04 recommended)
- GitHub account with your code
- Docker Hub account (free at hub.docker.com)
- Domain or IP for your EC2 server

---

## ═══════════════════════════════════
## PART 1: SET UP YOUR EC2 SERVER
## ═══════════════════════════════════

### Step 1.1 — SSH into your EC2 server

```bash
# On your LOCAL machine (Windows: use PuTTY or Git Bash)
ssh -i /path/to/your-key.pem ubuntu@YOUR_EC2_IP
```

### Step 1.2 — Update the server packages

```bash
# Always update first
sudo apt update && sudo apt upgrade -y
echo "✅ Server updated"
```

### Step 1.3 — Install Java 17

```bash
# Install Java (Spring Boot needs it)
sudo apt install -y openjdk-17-jdk

# Verify installation
java -version
# Expected output: openjdk version "17.x.x"
```

### Step 1.4 — Install Docker

```bash
# Install Docker
sudo apt install -y docker.io docker-compose

# Start Docker service
sudo systemctl start docker
sudo systemctl enable docker  # Auto-start on server reboot

# Allow 'ubuntu' user to run Docker without sudo
sudo usermod -aG docker ubuntu

# IMPORTANT: Log out and log back in for group change to take effect
exit
# Then SSH back in
ssh -i /path/to/your-key.pem ubuntu@YOUR_EC2_IP

# Verify Docker works
docker ps
# Should show empty table (no error)
echo "✅ Docker installed"
```

### Step 1.5 — Install Maven (for local builds)

```bash
sudo apt install -y maven

# Verify
mvn --version
echo "✅ Maven installed"
```

### Step 1.6 — Create deployment directory

```bash
# Create folder for your app
sudo mkdir -p /opt/shranvi-api
sudo mkdir -p /var/log/shranvi-api

# Give your user ownership
sudo chown -R ubuntu:ubuntu /opt/shranvi-api
sudo chown -R ubuntu:ubuntu /var/log/shranvi-api

echo "✅ Directories created"
```

### Step 1.7 — Open EC2 Security Group ports

Go to AWS Console → EC2 → Security Groups → Your instance's security group.

Add these INBOUND rules:
```
Type        Port    Source
SSH         22      Your IP (or 0.0.0.0/0 for anywhere)
Custom TCP  8081    0.0.0.0/0  ← Spring Boot API
Custom TCP  5000    0.0.0.0/0  ← Flask (already open)
Custom TCP  8080    0.0.0.0/0  ← Jenkins
Custom TCP  9000    0.0.0.0/0  ← SonarQube (optional)
```

---

## ═══════════════════════════════════
## PART 2: INSTALL JENKINS
## ═══════════════════════════════════

Jenkins is the automation server that runs your CI/CD pipeline.

### Step 2.1 — Install Jenkins

```bash
# Add Jenkins repository
curl -fsSL https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key | sudo tee \
    /usr/share/keyrings/jenkins-keyring.asc > /dev/null

echo deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] \
    https://pkg.jenkins.io/debian-stable binary/ | sudo tee \
    /etc/apt/sources.list.d/jenkins.list > /dev/null

sudo apt update
sudo apt install -y jenkins

# Start Jenkins
sudo systemctl start jenkins
sudo systemctl enable jenkins

echo "✅ Jenkins installed - runs on port 8080"
```

### Step 2.2 — Get initial Jenkins password

```bash
# This password is needed to unlock Jenkins for the first time
sudo cat /var/lib/jenkins/secrets/initialAdminPassword
# Copy this password!
```

### Step 2.3 — Configure Jenkins via browser

1. Open browser → go to: `http://YOUR_EC2_IP:8080`
2. Paste the password you copied above
3. Click "Install Suggested Plugins" (wait 2-3 minutes)
4. Create your admin username and password
5. Click "Save and Finish"

### Step 2.4 — Install required Jenkins plugins

In Jenkins:
1. Go to: `Manage Jenkins` → `Plugins` → `Available Plugins`
2. Search and install these plugins:
   - ✅ **Pipeline** (probably already installed)
   - ✅ **Git** (already installed)
   - ✅ **Docker Pipeline**
   - ✅ **SSH Agent**
   - ✅ **SonarQube Scanner**
   - ✅ **JaCoCo**
   - ✅ **HTML Publisher** (for OWASP reports)
   - ✅ **GitHub Integration**
3. Click "Install without restart"
4. Check "Restart Jenkins when installation is complete"

### Step 2.5 — Allow Jenkins to use Docker

```bash
# Jenkins runs as 'jenkins' user - give it Docker access
sudo usermod -aG docker jenkins
sudo systemctl restart jenkins
echo "✅ Jenkins can now run Docker commands"
```

---

## ═══════════════════════════════════
## PART 3: ADD CREDENTIALS TO JENKINS
## ═══════════════════════════════════

Credentials = passwords/keys stored securely in Jenkins (not in your code!)

### Step 3.1 — Add Docker Hub credentials

1. Jenkins → `Manage Jenkins` → `Credentials` → `System` → `Global credentials`
2. Click `Add Credentials`
3. Fill in:
   - Kind: **Username with password**
   - Username: your Docker Hub username
   - Password: your Docker Hub password
   - ID: `docker-hub-credentials`  ← Must match Jenkinsfile exactly!
   - Description: Docker Hub Login
4. Click Save

### Step 3.2 — Add EC2 SSH private key

1. Same place → `Add Credentials`
2. Fill in:
   - Kind: **SSH Username with private key**
   - Username: `ubuntu`
   - Private Key: Enter directly → paste contents of your `.pem` file
   - ID: `ec2-ssh-private-key`  ← Must match Jenkinsfile!
3. Click Save

```bash
# How to read your .pem file content (on your LOCAL machine):
cat /path/to/your-key.pem
# Copy everything including -----BEGIN RSA PRIVATE KEY----- lines
```

### Step 3.3 — Add SonarQube token

1. First create token in SonarQube: Login → My Account → Security → Generate Token
2. Then in Jenkins → Add Credentials:
   - Kind: **Secret text**
   - Secret: paste the token
   - ID: `sonarqube-token`

---

## ═══════════════════════════════════
## PART 4: CONFIGURE SONARQUBE (Optional but recommended)
## ═══════════════════════════════════

### Step 4.1 — Install SonarQube with Docker

```bash
# Run SonarQube as a Docker container on port 9000
docker run -d \
    --name sonarqube \
    --restart unless-stopped \
    -p 9000:9000 \
    -v sonarqube_data:/opt/sonarqube/data \
    sonarqube:community

echo "⏳ SonarQube starting... wait 2 minutes then open browser"
```

### Step 4.2 — Configure SonarQube in Jenkins

1. Jenkins → `Manage Jenkins` → `System`
2. Scroll to "SonarQube servers"
3. Click "Add SonarQube"
4. Fill in:
   - Name: `SonarQube`
   - Server URL: `http://localhost:9000`
   - Token: select `sonarqube-token` credential
5. Save

---

## ═══════════════════════════════════
## PART 5: CREATE JENKINS PIPELINE JOB
## ═══════════════════════════════════

### Step 5.1 — Create new Pipeline job

1. Jenkins → `New Item`
2. Enter name: `shranvi-products-api`
3. Select: **Pipeline**
4. Click OK

### Step 5.2 — Configure the job

In the job configuration page:

**General section:**
- ✅ Check "GitHub project"
- Project URL: `https://github.com/YOURUSERNAME/YOURREPO/`

**Build Triggers section:**
- ✅ Check "GitHub hook trigger for GITScm polling"
  (This makes Jenkins listen for GitHub webhooks)

**Pipeline section:**
- Definition: **Pipeline script from SCM**
- SCM: **Git**
- Repository URL: `https://github.com/YOURUSERNAME/YOURREPO.git`
- Branch: `*/main` (or `*/master`)
- Script Path: `Jenkinsfile`

Click **Save**

---

## ═══════════════════════════════════
## PART 6: SETUP GITHUB WEBHOOK
## ═══════════════════════════════════

This is what makes it AUTOMATIC - GitHub tells Jenkins when you push code.

### Step 6.1 — Add webhook in GitHub

1. Go to your GitHub repository
2. Settings → Webhooks → Add webhook
3. Fill in:
   - Payload URL: `http://YOUR_EC2_IP:8080/github-webhook/`
   - Content type: `application/json`
   - Which events: **Just the push event**
4. Click "Add webhook"

### Step 6.2 — Test the webhook

Push any small change to GitHub:
```bash
# On your local machine
echo "# test" >> README.md
git add README.md
git commit -m "Test webhook"
git push origin main
```

Go to Jenkins → you should see your pipeline start automatically! 🎉

---

## ═══════════════════════════════════
## PART 7: CONFIGURE YOUR SPRING BOOT APP
## ═══════════════════════════════════

### Step 7.1 — Update application.properties

Edit `src/main/resources/application.properties`:
```
# Replace <YOUR_SERVER_IP> with your actual MySQL server IP
spring.datasource.url=jdbc:mysql://YOUR_MYSQL_IP:3306/Shranvi6310?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=Shranvi6310
spring.datasource.password=//Shranvi6310//
```

### Step 7.2 — Update Jenkinsfile

Edit `Jenkinsfile` - change these values:
```groovy
DOCKER_IMAGE = "yourdockerhubusername/shranvi-products-api"  // ← Your Docker Hub username
EC2_HOST     = "13.201.XXX.XXX"  // ← Your EC2 server IP
```

### Step 7.3 — Update docker-compose.yml

```yaml
image: yourdockerhubusername/shranvi-products-api:latest  // ← Your Docker Hub username
```

---

## ═══════════════════════════════════
## PART 8: FIRST MANUAL DEPLOYMENT TEST
## ═══════════════════════════════════

Before relying on CI/CD, test manually once:

```bash
# On your EC2 server
cd /opt/shranvi-api

# Pull your image (replace with your Docker Hub username)
docker pull yourdockerhubusername/shranvi-products-api:latest

# Run it on port 8081
docker run -d \
    --name shranvi-api \
    --restart unless-stopped \
    -p 8081:8081 \
    yourdockerhubusername/shranvi-products-api:latest

# Check if it's running
docker ps

# Check logs
docker logs shranvi-api

# Test the API
curl http://localhost:8081/api/v1/products
curl http://localhost:8081/api/v1/products/health
```

---

## ═══════════════════════════════════
## PART 9: TEST YOUR API ENDPOINTS
## ═══════════════════════════════════

Once deployed, your API is accessible at:

```
Base URL: http://YOUR_EC2_IP:8081

📋 GET all products:
http://YOUR_EC2_IP:8081/api/v1/products

📋 GET active products only:
http://YOUR_EC2_IP:8081/api/v1/products/active

🔍 GET single product:
http://YOUR_EC2_IP:8081/api/v1/products/1

📂 GET by category:
http://YOUR_EC2_IP:8081/api/v1/products/category/Saree

🔎 Search products:
http://YOUR_EC2_IP:8081/api/v1/products/search?keyword=silk

💚 Health check:
http://YOUR_EC2_IP:8081/api/v1/products/health

📊 Actuator health:
http://YOUR_EC2_IP:8081/actuator/health
```

### Sample JSON Response:
```json
{
  "success": true,
  "message": "Products fetched successfully",
  "count": 25,
  "data": [
    {
      "id": 1,
      "name": "Banarasi Silk Saree",
      "description": "Premium silk with gold zari",
      "price": 4999.00,
      "category": "Saree",
      "stockQuantity": 50,
      "sku": "SAR-001",
      "isActive": true,
      "createdAt": "2024-01-15T10:30:00"
    }
  ]
}
```

---

## ═══════════════════════════════════
## PART 10: PORT SUMMARY (No Conflicts!)
## ═══════════════════════════════════

```
Service              Port    Status
─────────────────────────────────────
Flask App            5000    Already running ✅
Spring Boot API      8081    New - no conflict ✅
Jenkins              8080    CI/CD tool ✅
SonarQube            9000    Code quality ✅
MySQL                3306    Database ✅
```

---

## ═══════════════════════════════════
## COMMON PROBLEMS & SOLUTIONS
## ═══════════════════════════════════

### Problem: "Connection refused" to MySQL
```bash
# Check MySQL allows remote connections
# In MySQL server, run:
GRANT ALL PRIVILEGES ON Shranvi6310.* TO 'Shranvi6310'@'%' IDENTIFIED BY '//Shranvi6310//';
FLUSH PRIVILEGES;
```

### Problem: Jenkins can't connect to Docker
```bash
sudo usermod -aG docker jenkins
sudo systemctl restart jenkins
```

### Problem: Container starts then stops immediately
```bash
# Check logs
docker logs shranvi-api

# Most common reason: can't connect to MySQL
# Make sure MySQL IP in application.properties is correct
```

### Problem: Port 8081 not accessible
```bash
# Check EC2 security group allows port 8081
# Also check firewall on server:
sudo ufw allow 8081
```

### Problem: GitHub webhook not triggering
- Ensure Jenkins is accessible from internet (not just local network)
- Check webhook delivery in GitHub → Settings → Webhooks → Recent deliveries

---

## ═══════════════════════════════════
## HOW ENTERPRISE COMPANIES DO THIS
## ═══════════════════════════════════

What you're building is exactly how real companies work, just at larger scale:

```
YOUR SETUP (Beginner → Intermediate)     ENTERPRISE SCALE
──────────────────────────────────────   ──────────────────────────────────
GitHub (code hosting)                    GitHub Enterprise / GitLab
Jenkins (CI/CD)                          Jenkins / GitHub Actions / ArgoCD
Docker Hub (image registry)              AWS ECR / Azure ACR / JFrog
Single EC2 (deployment)                  Kubernetes cluster (EKS/AKS/GKE)
SonarQube (code quality)                 SonarQube Enterprise / Snyk
OWASP check (security)                   Snyk + Prisma Cloud + Aqua
application.properties (config)          AWS Secrets Manager / Vault
docker logs (monitoring)                 Datadog / Grafana + Prometheus
Manual DB migration                      Flyway / Liquibase

Your CI/CD pipeline is identical in concept to what Netflix,
Swiggy, Zepto, and other Indian tech companies use!
```

The only difference is scale - your pipeline follows all the same
best practices (build → test → quality gate → security → deploy).
```
