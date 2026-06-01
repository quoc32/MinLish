const authService = require('../services/authService');

/**
 * Register a new user
 * POST /api/auth/register
 */
async function register(req, res) {
  try {
    const { email, password, displayName, targetGoal, wordsPerDay } = req.body;

    if (!email || !password || !displayName) {
      return res.status(400).json({
        success: false,
        message: 'Missing required fields: email, password, displayName'
      });
    }

    const data = await authService.register({ email, password, displayName, targetGoal, wordsPerDay });

    res.status(201).json({
      success: true,
      message: 'User registered successfully.',
      data
    });
  } catch (err) {
    console.error('Registration API error:', err);
    res.status(err.message.includes('failed') ? 400 : 500).json({
      success: false,
      message: err.message || 'Internal server error during registration.'
    });
  }
}

/**
 * Login user
 * POST /api/auth/login
 */
async function login(req, res) {
  try {
    const { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json({
        success: false,
        message: 'Email and password are required.'
      });
    }

    const data = await authService.login({ email, password });

    res.status(200).json({
      success: true,
      message: 'Login successful.',
      data
    });
  } catch (err) {
    console.error('Login API error:', err);
    res.status(401).json({
      success: false,
      message: err.message || 'Invalid credentials or internal server error.'
    });
  }
}

/**
 * Google Login
 * POST /api/auth/google
 */
async function loginWithGoogle(req, res) {
  try {
    const { idToken } = req.body;

    if (!idToken) {
      return res.status(400).json({
        success: false,
        message: 'Google ID Token is required.'
      });
    }

    const data = await authService.loginWithGoogle({ idToken });

    res.status(200).json({
      success: true,
      message: 'Google login successful.',
      data
    });
  } catch (err) {
    console.error('Google Login API error:', err);
    res.status(401).json({
      success: false,
      message: err.message || 'Invalid Google token or server error.'
    });
  }
}

/**
 * Get user profile
 * GET /api/auth/profile
 */
async function getProfile(req, res) {
  try {
    const userId = req.user.id;
    const profile = await authService.getProfile(userId);

    res.status(200).json({
      success: true,
      data: profile
    });
  } catch (err) {
    console.error('Get profile error:', err);
    res.status(err.message === 'Profile not found.' ? 404 : 500).json({
      success: false,
      message: err.message || 'Internal server error fetching profile.'
    });
  }
}

/**
 * Update user profile
 * PUT /api/auth/profile
 */
async function updateProfile(req, res) {
  try {
    const userId = req.user.id;
    const profile = await authService.updateProfile(userId, req.body);

    res.status(200).json({
      success: true,
      message: 'Profile updated successfully.',
      data: profile
    });
  } catch (err) {
    console.error('Update profile API error:', err);
    res.status(400).json({
      success: false,
      message: err.message || 'Internal server error updating profile.'
    });
  }
}

async function forgotPassword(req, res) {
  try {
    const { email } = req.body;
    if (!email) {
      return res.status(400).json({
        success: false,
        message: 'Email is required.'
      });
    }

    await authService.forgotPassword(email);

    res.status(200).json({
      success: true,
      message: 'Password reset link sent to your email.'
    });
  } catch (err) {
    console.error('Forgot password API error:', err);
    res.status(400).json({
      success: false,
      message: err.message || 'Failed to request password reset.'
    });
  }
}

async function resetPassword(req, res) {
  try {
    const { password } = req.body;
    
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return res.status(401).json({
        success: false,
        message: 'No authorization token provided.'
      });
    }
    const token = authHeader.split(' ')[1];

    if (!password) {
      return res.status(400).json({
        success: false,
        message: 'New password is required.'
      });
    }

    await authService.resetPassword(token, password);

    res.status(200).json({
      success: true,
      message: 'Password has been reset successfully.'
    });
  } catch (err) {
    console.error('Reset password API error:', err);
    res.status(400).json({
      success: false,
      message: err.message || 'Failed to reset password.'
    });
  }
}

async function resetPasswordCallback(req, res) {
  const htmlContent = `<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đặt lại mật khẩu - MinLish</title>
    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;600;800&display=swap" rel="stylesheet">
    <style>
        :root {
            --bg-gradient: linear-gradient(135deg, #0f172a 0%, #1e1b4b 50%, #311042 100%);
            --card-bg: rgba(30, 30, 50, 0.45);
            --card-border: rgba(255, 255, 255, 0.08);
            --text-primary: #f8fafc;
            --text-secondary: #94a3b8;
            --primary-color: #6366f1;
            --primary-hover: #4f46e5;
            --glow-color: rgba(99, 102, 241, 0.15);
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }

        body {
            font-family: 'Outfit', sans-serif;
            background: var(--bg-gradient);
            color: var(--text-primary);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 24px;
            overflow: hidden;
        }

        /* Ambient glow spots */
        .glow-spot-1 {
            position: absolute;
            top: -10%;
            left: -10%;
            width: 50vw;
            height: 50vw;
            background: radial-gradient(circle, rgba(99, 102, 241, 0.15) 0%, rgba(0,0,0,0) 70%);
            filter: blur(80px);
            z-index: 1;
            pointer-events: none;
        }
        
        .glow-spot-2 {
            position: absolute;
            bottom: -10%;
            right: -10%;
            width: 50vw;
            height: 50vw;
            background: radial-gradient(circle, rgba(168, 85, 247, 0.12) 0%, rgba(0,0,0,0) 70%);
            filter: blur(80px);
            z-index: 1;
            pointer-events: none;
        }

        .container {
            position: relative;
            z-index: 10;
            width: 100%;
            max-width: 460px;
            background: var(--card-bg);
            backdrop-filter: blur(20px);
            -webkit-backdrop-filter: blur(20px);
            border: 1px solid var(--card-border);
            border-radius: 24px;
            padding: 40px;
            box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3), 
                        0 0 50px rgba(99, 102, 241, 0.05);
            text-align: center;
            transform: translateY(0);
            animation: fadeIn 0.8s cubic-bezier(0.16, 1, 0.3, 1) forwards;
        }

        @keyframes fadeIn {
            from {
                opacity: 0;
                transform: translateY(20px);
            }
            to {
                opacity: 1;
                transform: translateY(0);
            }
        }

        .logo {
            font-size: 32px;
            font-weight: 800;
            background: linear-gradient(135deg, #a5b4fc 0%, #6366f1 50%, #d8b4fe 100%);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            margin-bottom: 24px;
            letter-spacing: -0.5px;
            display: inline-block;
        }

        .icon-wrapper {
            width: 80px;
            height: 80px;
            background: rgba(99, 102, 241, 0.1);
            border: 1px solid rgba(99, 102, 241, 0.2);
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            margin: 0 auto 24px;
            position: relative;
        }

        .icon-wrapper::after {
            content: '';
            position: absolute;
            width: 100%;
            height: 100%;
            border-radius: 50%;
            border: 2px solid var(--primary-color);
            opacity: 0.4;
            animation: pulse 2s infinite;
        }

        @keyframes pulse {
            0% {
                transform: scale(1);
                opacity: 0.4;
            }
            70% {
                transform: scale(1.3);
                opacity: 0;
            }
            100% {
                transform: scale(1.3);
                opacity: 0;
            }
        }

        .icon {
            font-size: 36px;
        }

        h1 {
            font-size: 24px;
            font-weight: 600;
            margin-bottom: 12px;
            color: var(--text-primary);
        }

        p {
            font-size: 15px;
            color: var(--text-secondary);
            line-height: 1.6;
            margin-bottom: 32px;
        }

        .btn {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            width: 100%;
            padding: 16px 24px;
            background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
            border: none;
            border-radius: 14px;
            color: white;
            font-size: 16px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.2s ease;
            box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
            text-decoration: none;
        }

        .btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 20px rgba(99, 102, 241, 0.4);
            background: linear-gradient(135deg, #4f46e5 0%, #4338ca 100%);
        }

        .btn:active {
            transform: translateY(0);
        }

        .hint {
            margin-top: 24px;
            font-size: 12px;
            color: rgba(148, 163, 184, 0.6);
        }

        .hint a {
            color: var(--primary-color);
            text-decoration: none;
        }

        .hint a:hover {
            text-decoration: underline;
        }
    </style>
</head>
<body>
    <div class="glow-spot-1"></div>
    <div class="glow-spot-2"></div>

    <div class="container">
        <div class="logo">MinLish</div>
        <div class="icon-wrapper">
            <span class="icon">🔓</span>
        </div>
        <h1>Xác thực đặt lại mật khẩu</h1>
        <p id="status-text">Đang kết nối với ứng dụng MinLish để tiếp tục đặt lại mật khẩu...</p>
        
        <a id="redirect-btn" href="#" class="btn" style="display: none;">Mở ứng dụng MinLish</a>
        
        <div class="hint">
            Không thấy ứng dụng tự động mở? <br>
            Hãy nhấp vào nút ở trên hoặc <a href="javascript:void(0)" onclick="tryRedirect()">nhấp vào đây để thử lại</a>.
        </div>
    </div>

    <script>
        const hash = window.location.hash;
        const search = window.location.search;
        
        const deepLinkBase = 'minlish://reset-password';
        let finalUrl = deepLinkBase;
        
        if (hash) {
            finalUrl += hash;
        } else if (search) {
            finalUrl += search;
        }

        const statusText = document.getElementById('status-text');
        const redirectBtn = document.getElementById('redirect-btn');

        redirectBtn.href = finalUrl;

        function tryRedirect() {
            statusText.innerText = "Đang chuyển hướng bạn tới ứng dụng MinLish...";
            redirectBtn.style.display = 'inline-flex';
            
            window.location.href = finalUrl;
            
            setTimeout(() => {
                statusText.innerText = "Yêu cầu đặt lại mật khẩu đã sẵn sàng. Hãy nhấp vào nút dưới đây để tiếp tục trên ứng dụng MinLish.";
            }, 1500);
        }

        window.onload = function() {
            tryRedirect();
        };
    </script>
</body>
</html>`;

  res.setHeader('Content-Type', 'text/html');
  return res.send(htmlContent);
}

module.exports = {
  register,
  login,
  loginWithGoogle,
  getProfile,
  updateProfile,
  forgotPassword,
  resetPassword,
  resetPasswordCallback
};
