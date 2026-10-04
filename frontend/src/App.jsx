import React, { useEffect, useState } from "react";
import {
  Link,
  NavLink,
  Route,
  Routes,
  useLocation,
  useNavigate
} from "react-router-dom";
import {
  API_BASE_URL,
  downloadFileUrl,
  downloadMedia,
  getVideoInfo,
  sendContact,
  testBackend,
  validateUrl
} from "./api";

const PLATFORM_THEMES = {
  Instagram: {
    color: "#E1306C",
    soft: "#FDE8F1",
    border: "#F6B8D1",
    gradient: "linear-gradient(45deg, #F58529, #DD2A7B 45%, #8134AF 75%, #515BD4)"
  },
  YouTube: { color: "#FF0000", soft: "#FFEDED", border: "#FFBFBF" },
  Facebook: { color: "#1877F2", soft: "#E8F1FE", border: "#B9D5FB" },
  TikTok: { color: "#FE2C55", soft: "#FDE9EE", border: "#F9B3C2" },
  X: { color: "#0F1419", soft: "#E9EBEE", border: "#BEC4CC" }
};

const DEFAULT_THEME = { color: "#6d5dfc", soft: "#f0edff", border: "#dcd9f8" };

const PLATFORMS = [
  { name: "Instagram", domains: "instagram.com", icon: "◎" },
  { name: "YouTube", domains: "youtube.com · youtu.be", icon: "▶" },
  { name: "Facebook", domains: "facebook.com · fb.watch", icon: "f" },
  { name: "TikTok", domains: "tiktok.com", icon: "♪" },
  { name: "X", domains: "x.com · twitter.com", icon: "𝕏" }
];

function detectPlatform(text = "") {
  const u = String(text).toLowerCase();
  if (u.includes("instagram.com")) return "Instagram";
  if (u.includes("youtube.com") || u.includes("youtu.be")) return "YouTube";
  if (u.includes("facebook.com") || u.includes("fb.watch")) return "Facebook";
  if (u.includes("tiktok.com")) return "TikTok";
  if (u.includes("x.com") || u.includes("twitter.com")) return "X";
  return "";
}

const FAQS = [
  ["What does DoraClip do?", "DoraClip sends a publicly accessible media URL to its Spring Boot backend, which uses the configured downloader tools to prepare the requested file."],
  ["Which platforms are supported?", "The current frontend is prepared for Instagram, YouTube, Facebook, TikTok and X/Twitter URLs. Actual availability depends on the backend and the source URL."],
  ["Do I need to install anything?", "No. The visitor only needs a modern browser. The downloader tools run on the DoraClip server."],
  ["Where is the downloaded file saved?", "The backend temporarily creates the media file on the server. The browser then requests the file endpoint and saves the resulting file on the visitor's device. Server-side cleanup is handled by the backend."],
  ["Why can a download fail?", "A source may be private, restricted, unavailable, rate-limited, unsupported, or changed by the platform. Server configuration and downloader availability can also affect the result."],
  ["Is a database required?", "The downloader flow does not require a database for basic URL-to-file processing. A database can be added later for accounts, history, quotas, subscriptions or analytics."]
];

function Layout({ children }) {
  const location = useLocation();
  const [open, setOpen] = useState(false);

  useEffect(() => setOpen(false), [location.pathname]);

  return (
    <div className="site">
      <header className="header">
        <div className="container nav">
          <Link className="brand" to="/">
            <span className="logo">D</span>
            <span>DoraClip</span>
          </Link>

          <button className="menuBtn" onClick={() => setOpen(v => !v)} aria-label="Open menu">☰</button>

          <nav className={open ? "navLinks open" : "navLinks"}>
            <NavLink to="/" end>Home</NavLink>
            <NavLink to="/downloader">Downloader</NavLink>
            <NavLink to="/how-it-works">How it works</NavLink>
            <NavLink to="/faq">FAQ</NavLink>
            <NavLink className="navCta" to="/downloader">Start downloading</NavLink>
          </nav>
        </div>
      </header>

      {children}

      <footer className="footer">
        <div className="container footerGrid">
          <div>
            <Link className="brand" to="/"><span className="logo small">D</span><span>DoraClip</span></Link>
            <p>Simple downloading for publicly accessible media URLs.</p>
          </div>
          <div>
            <h4>Product</h4>
            <Link to="/downloader">Downloader</Link>
            <Link to="/how-it-works">How it works</Link>
            <Link to="/faq">FAQ</Link>
          </div>
          <div>
            <h4>Information</h4>
            <Link to="/about">About</Link>
            <Link to="/privacy">Privacy</Link>
            <Link to="/terms">Terms</Link>
            <Link to="/contact">Contact</Link>
          </div>
        </div>
        <div className="container footerBottom">
          <span>© {new Date().getFullYear()} DoraClip</span>
          <span>Use content only when you have permission or rights to save it.</span>
        </div>
      </footer>
    </div>
  );
}

function Home() {
  const [activePlatform, setActivePlatform] = useState("");
  return (
    <>
      <main>
        <section className="hero container">
          <div className="heroText">
            <span className="eyebrow">MULTI-PLATFORM DOWNLOADER</span>
            <h1>Download social media videos <em>simply.</em></h1>
            <p>
              Paste a public video URL, check it, choose video or audio,
              and let DoraClip prepare the file.
            </p>
            <div className="heroActions">
              <Link className="button primary" to="/downloader">Open downloader</Link>
              <Link className="button secondary" to="/how-it-works">How it works</Link>
            </div>
            <div className="trustRow">
              <span>✓ No account required</span>
              <span>✓ Browser based</span>
              <span>✓ Temporary server files</span>
            </div>
          </div>
          <DownloaderCard compact onPlatformChange={setActivePlatform} />
        </section>

        <section className="section container">
          <div className="sectionHead">
            <span className="eyebrow">SUPPORTED PLATFORMS</span>
            <h2>One place for everyday downloads.</h2>
            <p>Enter a URL from a supported platform and DoraClip will detect it.</p>
          </div>
          <PlatformGrid active={activePlatform} />
        </section>

        <section className="section soft">
          <div className="container featureGrid">
            <div>
              <span className="eyebrow">WHY DORACLIP</span>
              <h2>A straightforward downloader experience.</h2>
            </div>
            <div className="featureCards">
              <Feature icon="01" title="Paste a URL" text="No complicated forms. Start with the public media URL." />
              <Feature icon="02" title="Choose a format" text="Request video or audio through the existing backend API." />
              <Feature icon="03" title="Download the file" text="The browser receives the prepared file from the backend." />
            </div>
          </div>
        </section>
      </main>
    </>
  );
}

function Feature({ icon, title, text }) {
  return <div className="feature"><b>{icon}</b><div><h3>{title}</h3><p>{text}</p></div></div>;
}

function PlatformGrid({ active = "" }) {
  return (
    <div className="platformGrid">
      {PLATFORMS.map(p => {
        const theme = PLATFORM_THEMES[p.name];
        const isActive = active === p.name;
        return (
          <div
            className={isActive ? "platformCard active" : "platformCard"}
            key={p.name}
            data-platform={p.name}
            style={isActive ? {
              "--card-accent": theme.color,
              "--card-soft": theme.soft,
              "--card-border": theme.border
            } : undefined}
          >
            <span className="platformIcon">{p.icon}</span>
            <strong>{p.name}</strong>
            <small>{p.domains}</small>
            {isActive && <span className="platformActiveDot">● Active</span>}
          </div>
        );
      })}
    </div>
  );
}

function Downloader() {
  const [activePlatform, setActivePlatform] = useState("");
  return (
    <main>
      <section className="pageHero container">
        <span className="eyebrow">DORACLIP DOWNLOADER</span>
        <h1>Paste. Check. Download.</h1>
        <p>Use a publicly accessible URL from one of the supported platforms.</p>
      </section>
      <section className="container downloaderSection">
        <DownloaderCard onPlatformChange={setActivePlatform} />
        <div className="sideInfo">
          <h3>Supported platforms</h3>
          <PlatformGrid active={activePlatform} />
          <p className="note">The backend ultimately determines whether a specific URL can be downloaded.</p>
        </div>
      </section>
    </main>
  );
}

function DownloaderCard({ compact = false, onPlatformChange }) {
  const [url, setUrl] = useState("");
  const [mode, setMode] = useState("video");
  const [quality, setQuality] = useState("720p");
  const [audioFormat, setAudioFormat] = useState("mp3");
  const [status, setStatus] = useState("idle");
  const [message, setMessage] = useState("");
  const [result, setResult] = useState(null);
  const [info, setInfo] = useState(null);
  const [platform, setPlatform] = useState("");

  const busy = status === "checking" || status === "downloading";
  const theme = PLATFORM_THEMES[platform] || DEFAULT_THEME;

  // Live auto-detect on paste / type: theme the download section instantly.
  useEffect(() => {
    const found = detectPlatform(url);
    setPlatform(found);
    if (onPlatformChange) onPlatformChange(found);
  }, [url]);

  function detect(text) {
    return detectPlatform(text);
  }

  function clearOutput() {
    setMessage("");
    setResult(null);
    setInfo(null);
  }

  async function check() {
    const clean = url.trim();
    clearOutput();
    if (!clean) {
      setStatus("error"); setMessage("Please paste a URL first."); return;
    }
    setStatus("checking");
    setPlatform(detect(clean));
    setMessage("Checking URL...");
    try {
      const response = await validateUrl(clean);
      setInfo(response);
      setStatus("ready");
      setMessage(response?.message || "URL is ready.");
    } catch (e) {
      setStatus("error"); setMessage(e.message || "URL validation failed.");
    }
  }

  async function showInfo() {
    const clean = url.trim();
    if (!clean) {
      setStatus("error"); setMessage("Please paste a URL first."); return;
    }
    setStatus("checking");
    setMessage("Reading media information...");
    try {
      const response = await getVideoInfo(clean);
      setInfo(response);
      setPlatform(detect(clean));
      setStatus("ready");
      setMessage("Media information loaded.");
    } catch (e) {
      setStatus("error"); setMessage(e.message || "Could not load media information.");
    }
  }

  async function startDownload() {
    const clean = url.trim();
    if (!clean) {
      setStatus("error"); setMessage("Please paste a URL first."); return;
    }

    setStatus("downloading");
    setResult(null);
    setMessage("Preparing your download. This can take a little while...");
    setPlatform(detect(clean));

    try {
      const response = await downloadMedia({
        url: clean,
        mode,
        quality,
        audioFormat
      });

      const data = response?.data || response || {};
      const fileName = data.fileName || data.filename || data.name;

      if (!fileName) {
        throw new Error(response?.message || "The backend did not return a downloadable file name.");
      }

      setResult({ fileName, response });
      setStatus("success");
      setMessage("File prepared. Starting browser download...");

      // Important: use the backend file endpoint directly.
      // This avoids downloading the entire file into frontend memory.
      const link = document.createElement("a");
      link.href = downloadFileUrl(fileName);
      link.download = fileName;
      link.target = "_self";
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (e) {
      setStatus("error");
      setMessage(e.message || "Media download failed.");
    }
  }

  return (
    <div
      className={compact ? "downloadCard compact" : "downloadCard"}
      data-platform={platform || "default"}
      style={{
        "--accent": theme.color,
        "--accent-soft": theme.soft,
        "--accent-border": theme.border
      }}
    >
      <div className="downloadCardTop">
        <div>
          <span className="eyebrow tiny">DOWNLOAD MEDIA</span>
          <h2>{compact ? "Try DoraClip" : "Download a video or audio file"}</h2>
        </div>
        {platform
          ? <span className="detected" style={{ background: theme.soft, borderColor: theme.border, color: theme.color }}>✓ {platform}</span>
          : <span className="detected muted">Paste a link</span>}
      </div>

      <label className="fieldLabel" htmlFor={compact ? "homeUrl" : "downloadUrl"}>Media URL</label>
      <div className="urlInput">
        <input
          id={compact ? "homeUrl" : "downloadUrl"}
          value={url}
          onChange={e => { setUrl(e.target.value); if (status !== "idle") clearOutput(); }}
          onPaste={e => {
            const pasted = e.clipboardData?.getData("text") || "";
            if (pasted) {
              // Let the input update first, then force theme instantly for paste.
              const found = detectPlatform(pasted);
              if (found) {
                setPlatform(found);
                if (onPlatformChange) onPlatformChange(found);
              }
            }
          }}
          onKeyDown={e => e.key === "Enter" && check()}
          placeholder="https://www.youtube.com/watch?v=..."
          autoComplete="off"
        />
        <button className="button primary" onClick={check} disabled={busy}>
          {status === "checking" ? "Checking..." : "Check URL"}
        </button>
      </div>

      <div className="downloadControls">
        <div>
          <span className="fieldLabel">Type</span>
          <div className="toggle">
            <button className={mode === "video" ? "selected" : ""} onClick={() => setMode("video")}>Video</button>
            <button className={mode === "audio" ? "selected" : ""} onClick={() => setMode("audio")}>Audio</button>
          </div>
        </div>
        <div>
          <label className="fieldLabel" htmlFor="quality">Quality</label>
          <select id="quality" value={quality} disabled={mode === "audio"} onChange={e => setQuality(e.target.value)}>
            <option value="best">Best available</option>
            <option value="1080p">1080p</option>
            <option value="720p">720p</option>
            <option value="480p">480p</option>
            <option value="360p">360p</option>
          </select>
        </div>
        {mode === "audio" && (
          <div>
            <label className="fieldLabel" htmlFor="audioFormat">Audio format</label>
            <select id="audioFormat" value={audioFormat} onChange={e => setAudioFormat(e.target.value)}>
              <option value="mp3">MP3</option>
              <option value="m4a">M4A</option>
            </select>
          </div>
        )}
      </div>

      {message && (
        <div className={`status ${status === "error" ? "statusError" : status === "success" ? "statusSuccess" : ""}`}>
          {message}
        </div>
      )}

      <div className="downloadActions">
        <button className="button primary large" onClick={startDownload} disabled={busy}>
          {status === "downloading" ? "Downloading..." : "Download"}
        </button>
        <button className="outlineButton" onClick={showInfo} disabled={busy}>Get media info</button>
      </div>

      {result && (
        <div className="fileReady">
          <strong>Download prepared</strong>
          <span>{result.fileName}</span>
          <a className="manualLink" href={downloadFileUrl(result.fileName)}>Click here if the download did not start</a>
        </div>
      )}

      {info && !compact && (
        <details className="responseDetails">
          <summary>Backend response</summary>
          <pre>{JSON.stringify(info, null, 2)}</pre>
        </details>
      )}

      <p className="disclaimer">
        Only download content you have permission or rights to save. Public availability does not transfer copyright.
      </p>
    </div>
  );
}

function HowItWorks() {
  return (
    <main>
      <section className="pageHero container">
        <span className="eyebrow">HOW IT WORKS</span>
        <h1>A simple browser-to-backend flow.</h1>
        <p>DoraClip keeps the frontend focused on the user interface while the Spring Boot backend handles media processing.</p>
      </section>
      <section className="container stepsGrid">
        {[
          ["01", "Paste a URL", "Enter a publicly accessible URL in the downloader."],
          ["02", "Validate it", "The frontend sends the URL to POST /api/download/validate."],
          ["03", "Choose options", "Select video or audio and a quality where applicable."],
          ["04", "Prepare media", "POST /api/download asks the backend to process the media."],
          ["05", "Receive the file", "The frontend uses the returned fileName with GET /api/download/file."],
          ["06", "Temporary server file", "The backend can clean its temporary download directory after delivery."]
        ].map(([n, title, text]) => (
          <div className="stepCard" key={n}>
            <span>{n}</span><h3>{title}</h3><p>{text}</p>
          </div>
        ))}
      </section>
    </main>
  );
}

function FAQ() {
  const [active, setActive] = useState(null);
  return (
    <main>
      <section className="pageHero container">
        <span className="eyebrow">FAQ</span>
        <h1>Frequently asked questions.</h1>
        <p>Answers about the DoraClip downloader and its browser-to-backend flow.</p>
      </section>
      <section className="container faqList">
        {FAQS.map(([q, a], i) => (
          <button className="faqItem" key={q} onClick={() => setActive(active === i ? null : i)}>
            <span>{q}</span><b>{active === i ? "−" : "+"}</b>
            {active === i && <p>{a}</p>}
          </button>
        ))}
      </section>
    </main>
  );
}

function StaticPage({ eyebrow, title, children }) {
  return (
    <main>
      <section className="pageHero container">
        <span className="eyebrow">{eyebrow}</span>
        <h1>{title}</h1>
      </section>
      <section className="container prose">{children}</section>
    </main>
  );
}

function About() {
  return <StaticPage eyebrow="ABOUT" title="About DoraClip">
    <p>DoraClip is a browser-based interface for requesting downloads of publicly accessible media URLs through a server-side downloader.</p>
    <h2>How the service is structured</h2>
    <p>The React frontend collects the URL and download preferences. The Spring Boot backend validates the request, invokes its configured media tools, and exposes the resulting file through its download endpoint.</p>
    <p>The frontend does not need direct access to yt-dlp or FFmpeg. Those tools remain on the backend machine.</p>
  </StaticPage>;
}

function Privacy() {
  return <StaticPage eyebrow="PRIVACY" title="Privacy">
    <p>This page is a starter privacy page for the local DoraClip application. Before production launch, replace this text with your actual privacy policy and describe the exact data, logs, cookies, analytics and third-party services your deployment uses.</p>
    <h2>URLs and temporary files</h2>
    <p>The downloader needs the URL submitted by the visitor to process the requested media. The backend may create a temporary server-side file before returning it to the browser.</p>
    <h2>Production notice</h2>
    <p>Configure this page to match your real infrastructure, retention periods, analytics configuration and applicable legal requirements.</p>
  </StaticPage>;
}

function Terms() {
  return <StaticPage eyebrow="TERMS" title="Terms of use">
    <p>Use DoraClip only for content you are legally permitted to access and save. Do not use the service to bypass private access controls or other restrictions.</p>
    <h2>Availability</h2>
    <p>Media platforms can change their URLs, access rules, rate limits and delivery mechanisms. DoraClip therefore cannot guarantee that every public URL will always be downloadable.</p>
    <h2>Responsibility</h2>
    <p>You are responsible for the content you request and for complying with applicable laws, platform terms and copyright requirements.</p>
  </StaticPage>;
}

const ICONS = {
  user: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2" /><circle cx="12" cy="7" r="4" /></svg>,
  mail: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect width="20" height="16" x="2" y="4" rx="2" /><path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7" /></svg>,
  subject: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" /></svg>,
  message: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z" /><path d="M14 2v4a2 2 0 0 0 2 2h4" /><path d="M10 9H8" /><path d="M16 13H8" /><path d="M16 17H8" /></svg>,
  send: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="m22 2-7 20-4-9-9-4Z" /><path d="M22 2 11 13" /></svg>
};

function Contact() {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [subject, setSubject] = useState("");
  const [message, setMessage] = useState("");
  const [status, setStatus] = useState("idle");
  const [note, setNote] = useState("");

  const busy = status === "sending";

  async function submit(e) {
    e.preventDefault();
    if (busy) return;
    if (!name.trim() || !email.trim() || !message.trim()) {
      setStatus("error");
      setNote("Please fill in your name, email and message.");
      return;
    }
    setStatus("sending");
    setNote("Sending your message...");
    try {
      const res = await sendContact({
        name: name.trim(),
        email: email.trim(),
        subject: subject.trim(),
        message: message.trim()
      });
      setStatus("success");
      setNote(res?.message || "Thanks! Your message has been sent.");
      setName("");
      setEmail("");
      setSubject("");
      setMessage("");
    } catch (err) {
      setStatus("error");
      setNote(err.message || "Could not send your message. Please try again.");
    }
  }

  return (
    <main>
      <section className="pageHero container">
        <span className="eyebrow">CONTACT</span>
        <h1>Contact DoraClip</h1>
        <p>Have a question, feedback, or need help? We'd love to hear from you. Send us a message and we'll get back to you as soon as possible.</p>
      </section>
      <section className="container prose">
        <form className="contactCard" onSubmit={submit} noValidate>
          <div className="contactRow">
            <div className="contactField">
              <span>Full Name</span>
              <div className="inputWrap">{ICONS.user}
                <input value={name} onChange={e => setName(e.target.value)} placeholder="Enter your full name" autoComplete="name" />
              </div>
            </div>
            <div className="contactField">
              <span>Email Address</span>
              <div className="inputWrap">{ICONS.mail}
                <input type="email" value={email} onChange={e => setEmail(e.target.value)} placeholder="you@example.com" autoComplete="email" />
              </div>
            </div>
          </div>
          <div className="contactField">
            <span>Subject</span>
            <div className="inputWrap">{ICONS.subject}
              <input value={subject} onChange={e => setSubject(e.target.value)} placeholder="How can we help?" />
            </div>
          </div>
          <div className="contactField">
            <span>Message</span>
            <div className="inputWrap textareaWrap">{ICONS.message}
              <textarea value={message} onChange={e => setMessage(e.target.value)} placeholder="Write your message here..." />
            </div>
          </div>
          {note && (
            <div className={`status ${status === "error" ? "statusError" : status === "success" ? "statusSuccess" : ""}`}>
              {note}
            </div>
          )}
          <div className="downloadActions">
            <button type="submit" className="sendBtn" disabled={busy}>
              {ICONS.send} {busy ? "Sending..." : "Send Message"}
            </button>
          </div>
        </form>
      </section>
    </main>
  );
}

function BackendStatus() {
  const [state, setState] = useState("checking");
  useEffect(() => {
    testBackend().then(() => setState("online")).catch(() => setState("offline"));
  }, []);
  return <span className={`backendBadge ${state}`}>{state === "online" ? "● Backend online" : state === "offline" ? "● Backend offline" : "● Checking backend"}</span>;
}

function App() {
  const location = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
    const titles = {
      "/": "DoraClip — Multi-Platform Video Downloader",
      "/downloader": "DoraClip Downloader",
      "/how-it-works": "How DoraClip Works",
      "/faq": "DoraClip FAQ",
      "/about": "About DoraClip",
      "/privacy": "DoraClip Privacy",
      "/terms": "DoraClip Terms",
      "/contact": "Contact DoraClip"
    };
    document.title = titles[location.pathname] || "DoraClip";
  }, [location.pathname]);

  return (
    <Layout>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/downloader" element={<Downloader />} />
        <Route path="/how-it-works" element={<HowItWorks />} />
        <Route path="/faq" element={<FAQ />} />
        <Route path="/about" element={<About />} />
        <Route path="/privacy" element={<Privacy />} />
        <Route path="/terms" element={<Terms />} />
        <Route path="/contact" element={<Contact />} />
        <Route path="*" element={<NotFound go={() => navigate("/")} />} />
      </Routes>
    </Layout>
  );
}

function NotFound({ go }) {
  return <main><section className="pageHero container"><span className="eyebrow">404</span><h1>Page not found.</h1><button className="button primary" onClick={go}>Go home</button></section></main>;
}

export default App;