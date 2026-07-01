import urllib.request, urllib.parse, http.cookiejar

base = 'http://localhost:8081'
jar = http.cookiejar.CookieJar()
opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))
opener.addheaders = [('User-Agent', 'Mozilla/5.0')]

# GET login page
with opener.open(base + '/login') as resp:
    login_html = resp.read().decode('utf-8', errors='ignore')
    print('GET /login ->', resp.status)

# POST login credentials
payload = urllib.parse.urlencode({
    'username': 'student1',
    'password': 'student123'
}).encode('utf-8')
req = urllib.request.Request(base + '/login', data=payload, method='POST')
req.add_header('Content-Type', 'application/x-www-form-urlencoded')
with opener.open(req) as resp:
    post_body = resp.read().decode('utf-8', errors='ignore')
    print('POST /login ->', resp.status)
    print('POST final url ->', resp.geturl())

# Try protected page
with opener.open(base + '/student/scan') as resp:
    scan_body = resp.read().decode('utf-8', errors='ignore')
    print('GET /student/scan ->', resp.status)
    print('SCAN final url ->', resp.geturl())

print('Contains scanForm:', 'id="scanForm"' in scan_body)
print('Contains Start Camera:', 'Start Camera' in scan_body)
print('Contains profile-trigger:', 'profile-trigger' in scan_body)
print('Contains error alert:', 'Invalid username or password' in post_body)
print('Contains logout success:', 'logged out' in post_body)
