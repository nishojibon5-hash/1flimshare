import React, { useState } from 'react'
import { 
  Download, 
  Play, 
  Flame, 
  Star, 
  ShieldAlert, 
  Check, 
  HelpCircle, 
  Globe, 
  ChevronDown, 
  ChevronUp, 
  Tv, 
  Wifi, 
  Film, 
  Shield, 
  Smartphone, 
  Eye, 
  Share2, 
  Info,
  ArrowRight
} from 'lucide-react'

// APK Download link from AGENTS.md upload task
const APK_DOWNLOAD_URL = "https://tmpfiles.org/dl/wKwmbPjS9I6z/flimshare_v1.0.apk"
const APK_SIZE = "18 MB"
const APP_VERSION = "v1.0.0"

export default function App() {
  const [lang, setLang] = useState('bn') // 'bn' or 'en'
  const [activeFaq, setActiveFaq] = useState(null)
  const [selectedMovie, setSelectedMovie] = useState(null) // For the enticing detail popup

  const t = (bnText, enText) => {
    return lang === 'bn' ? bnText : enText
  }

  // Categories data
  const categories = [
    {
      id: "bangla_movies",
      title: t("🎬 বাংলা ট্রেন্ডিং মুভি", "🎬 Bangla Trending Movies"),
      desc: t("ঢালিউড ও বলিউড • ২০২৬ লেটেস্ট মুভি • সম্পূর্ণ ফ্রিতে দেখুন", "Dhallywood & Bollywood • 2026 Latest Movies • All FREE to watch"),
      items: [
        {
          title: t("হাওয়া (Hawa)", "Hawa"),
          year: "2026",
          genre: t("রহস্য ও থ্রিলার", "Mystery Thriller"),
          badges: ["HD", t("ফ্রি", "FREE")],
          image: "https://images.unsplash.com/photo-1518156677180-95a2893f3e9f?auto=format&fit=crop&w=600&q=80", // Ocean storm
          desc: t("গভীর সমুদ্রে মাছ ধরার ট্রলারে এক রহস্যময় তরুণীর আগমন এবং তা ঘিরে ঘটে যাওয়া অবিশ্বাস্য রহস্যের গল্প।", "A mysterious young woman appears on a deep-sea fishing trawler, triggering unbelievable mysteries.")
        },
        {
          title: t("সুরঙ্গ (Surongo)", "Surongo"),
          year: "2026",
          genre: t("ক্রাইম থ্রিলার", "Heist Thriller"),
          badges: ["4K", t("ফ্রি", "FREE")],
          image: "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?auto=format&fit=crop&w=600&q=80", // Dark cave
          desc: t("অভাবের তাড়নায় এক সাধারণ মানুষের ব্যাংক ডাকাতির দুঃসাহসিক এবং অবিশ্বাস্য সুড়ঙ্গ রহস্যের কাহিনী।", "An epic and nail-biting heist story of an ordinary man driven by poverty to dig a tunnel into a bank vault.")
        },
        {
          title: t("প্রিয়তমা (Priyotama)", "Priyotama"),
          year: "2026",
          genre: t("রোমান্টিক ড্রামা", "Romance Drama"),
          badges: ["4K", t("ফ্রি", "FREE")],
          image: "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?auto=format&fit=crop&w=600&q=80", // Golden sunset
          desc: t("শাকিব খানের ক্যারিয়ারের অন্যতম সেরা ট্র্যাজিক এবং অমর প্রেমের হৃদয়স্পর্শী কাহিনী।", "A touching romantic tragedy showcasing Shakib Khan's career-best performance in an eternal love story.")
        },
        {
          title: t("তুফান (Toofan)", "Toofan"),
          year: "2026",
          genre: t("অ্যাকশন থ্রিলার", "Action Thriller"),
          badges: ["HD", t("নতুন", "NEW")],
          image: "https://images.unsplash.com/photo-1578301978693-85fa9c0320b9?auto=format&fit=crop&w=600&q=80", // Industrial crime vibe
          desc: t("আশির দশকের গ্যাংস্টার রাজত্বের দুর্ধর্ষ উত্থান ও রাজকীয় অ্যাকশন ড্রামা।", "The action-packed rise of an 80s criminal kingpin in a stellar visual masterpiece.")
        }
      ]
    },
    {
      id: "live_sports",
      title: t("🏏 সরাসরি খেলাধুলা (Live Sports)", "🏏 Live Sports"),
      desc: t("বাংলাদেশ ক্রিকেট • বিপিএল • আইপিএল • লাইভ ফুটবল • সব খেলা ফ্রি", "Bangladesh Cricket • BPL T20 • IPL • Football • All LIVE matches FREE"),
      items: [
        {
          title: t("বাংলাদেশ বনাম ভারত লাইভ", "Bangladesh vs India Live"),
          year: "LIVE NOW",
          genre: t("আন্তর্জাতিক ক্রিকেট", "International Cricket"),
          badges: ["LIVE", t("ফ্রি", "FREE")],
          image: "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?auto=format&fit=crop&w=600&q=80", // Cricket stadium
          desc: t("বাংলাদেশ বনাম ভারতের টানটান উত্তেজনার ক্রিকেট সিরিজ সরাসরি ফ্রিতে দেখুন বাফারিং ছাড়া।", "Watch the high-voltage clash between Bangladesh and India live for free with zero buffering.")
        },
        {
          title: t("আইপিএল ২০২৬ লাইভ (IPL)", "IPL 2026 Live"),
          year: "LIVE NOW",
          genre: t("টি-টোয়েন্টি টুর্নামেন্ট", "T20 Tournament"),
          badges: ["LIVE", t("ফ্রি", "FREE")],
          image: "https://images.unsplash.com/photo-1540747737956-378724044492?auto=format&fit=crop&w=600&q=80", // Cricket match lights
          desc: t("আইপিএল ২০২৬ এর প্রতিটি ম্যাচ সরাসরি বাংলা ধারাভাষ্য সহ হাই কোয়ালিটিতে উপভোগ করুন।", "Enjoy every match of IPL 2026 live with Bengali and English commentary in ultra HD.")
        },
        {
          title: t("চ্যাম্পিয়ন্স লীগ ফুটবল", "UEFA Champions League"),
          year: "LIVE NOW",
          genre: t("ফুটবল টুর্নামেন্ট", "Football Tournament"),
          badges: ["LIVE", t("ফ্রি", "FREE")],
          image: "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?auto=format&fit=crop&w=600&q=80", // Soccer match kick-off
          desc: t("ইউরোপিয়ান ফুটবল রাজত্বের চ্যাম্পিয়ন্স লীগের সমস্ত ম্যাচ লাইভ স্ট্রিম করুন একদম ফ্রিতে।", "Stream all matches of UEFA Champions League live and free in stunning HD quality.")
        },
        {
          title: t("বিপিএল টি-টোয়েন্টি (BPL)", "BPL T20 Live"),
          year: "LIVE NOW",
          genre: t("বাংলাদেশ প্রিমিয়ার লীগ", "Bangladesh Premier League"),
          badges: ["LIVE", t("ফ্রি", "FREE")],
          image: "https://images.unsplash.com/photo-1531415074968-036ba1b575da?auto=format&fit=crop&w=600&q=80", // Cricket ground
          desc: t("বাংলাদেশ প্রিমিয়ার লীগ (বিপিএল) ক্রিকেট টুর্নামেন্টের সমস্ত ম্যাচ লাইভ দেখতে এখনই অ্যাপটি ইন্সটল করুন।", "Stream every single match of BPL T20 live on your mobile with high-speed multi-channel streaming.")
        }
      ]
    },
    {
      id: "bangla_shows",
      title: t("🎭 বাংলা নাটক ও রিয়েলিটি শো", "🎭 Bangla Natok & Reality Shows"),
      desc: t("বিগ বস বাংলা • সা রে গা মা পা • বিটিভি নাটক • কমেডি নাটক • সব ফ্রি", "Bigg Boss Bangla • Sa Re Ga Ma Pa • BTV Natok • Comedy Natok • All FREE"),
      items: [
        {
          title: t("বিগ বস বাংলা (Bigg Boss)", "Bigg Boss Bangla"),
          year: "2026",
          genre: t("রিয়েলিটি শো", "Reality Show"),
          badges: ["LIVE", t("ফ্রি", "FREE")],
          image: "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?auto=format&fit=crop&w=600&q=80", // Entertainment stage
          desc: t("টানটান ড্রামা এবং বির্তকে ভরা বিগ বস বাংলা লাইভ দেখতে যুক্ত থাকুন।", "Watch the drama-filled, sensational episodes of Bigg Boss Bangla live or catch up on-demand.")
        },
        {
          title: t("ড্যান্স বাংলাদেশ ড্যান্স", "Dance Bangladesh"),
          year: "2026",
          genre: t("নাচ রিয়েলিটি শো", "Dance Reality Show"),
          badges: ["HD", t("ফ্রি", "FREE")],
          image: "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?auto=format&fit=crop&w=600&q=80", // Stage dance
          desc: t("বাংলার সেরা নাচের রিয়েলিটি শো ড্যান্স বাংলাদেশ ড্যান্স-এর নতুন সিজন দেখুন।", "Stream the energetic dance battles from Bengal's favorite dance competition.")
        },
        {
          title: t("সা রে গা মা পা ২০২৬", "Sa Re Ga Ma Pa 2026"),
          year: "2026",
          genre: t("গান রিয়েলিটি শো", "Music Reality Show"),
          badges: ["LIVE", t("ফ্রি", "FREE")],
          image: "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?auto=format&fit=crop&w=600&q=80", // Singing microphone
          desc: t("সারেগামাপা ২০২৬ এর সঙ্গীত লড়াই এবং জাদুকরী সব পারফরম্যান্স সরাসরি লাইভ দেখুন।", "Listen to pure vocal magic and musical duals in the new season of Sa Re Ga Ma Pa.")
        },
        {
          title: t("ফিমেল ৪ (Female 4)", "Female 4 (Special Natok)"),
          year: "2026",
          genre: t("বাংলা কমেডি নাটক", "Bangla Comedy Natok"),
          badges: ["HD", t("নতুন", "NEW")],
          image: "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?auto=format&fit=crop&w=600&q=80", // Cinema theater / family drama
          desc: t("জনপ্রিয় ফ্র্যাঞ্চাইজি ফিমেল-এর চতুর্থ কিস্তি নিয়ে হাস্যরসে ভরপুর তুমুল জনপ্রিয় স্পেশাল নাটক।", "The hilarious fourth installment of the famous comedy drama series Female.")
        }
      ]
    },
    {
      id: "indian_serials",
      title: t("🇮🇳 ইন্ডিয়ান বাংলা ও হিন্দি সিরিয়াল", "🇮🇳 Indian Bengali & Hindi Serials"),
      desc: t("স্টার জলসা • জি বাংলা • অনুপমা • নাগিন • কালার্স • সব ফ্রি", "Star Jalsha • Zee Bangla • Anupamaa • Naagin • Colors • All FREE"),
      items: [
        {
          title: t("অনুরাগের ছোঁয়া (Star Jalsha)", "Anurager Chhowa"),
          year: "2026",
          genre: t("পারিবারিক নাটক", "Family Drama"),
          badges: ["TOP", t("ফ্রি", "FREE")],
          image: "https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&w=600&q=80", // Indian ethnic fashion/bridal
          desc: t("স্টার জলসার তুমুল জনপ্রিয় সিরিয়াল অনুরাগের ছোঁয়া-এর আজকের পর্ব টিভির আগে অ্যাপে দেখুন।", "Watch the emotionally gripping episodes of Anurager Chhowa on your phone before TV.")
        },
        {
          title: t("মিঠাই (Mithai Zee Bangla)", "Mithai (Zee Bangla)"),
          year: "2026",
          genre: t("রোমান্টিক কমেডি", "Romantic Comedy"),
          badges: ["HD", t("ফ্রি", "FREE")],
          image: "https://images.unsplash.com/photo-1563245372-f21724e3856d?auto=format&fit=crop&w=600&q=80", // Sweet / Festival
          desc: t("মিষ্টি ও দুষ্টু মিঠাইয়ের মিষ্টি প্রেম এবং পারিবারিক ড্রামা দেখুন একদম ফ্রিতে।", "Follow the sweet and bubbly story of Mithai and her loving family.")
        },
        {
          title: t("নাগিন ৭ (Colors TV)", "Naagin 7"),
          year: "2026",
          genre: t("ফ্যান্টাসি থ্রিলার", "Fantasy Thriller"),
          badges: ["NEW", t("ফ্রি", "FREE")],
          image: "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?auto=format&fit=crop&w=600&q=80", // Mystical female anime
          desc: t("কালার্স টিভির রহস্যময় অতিপ্রাকৃতিক নাটক নাগিন ৭ এর জমজমাট অলৌকিক নতুন গল্প।", "The mythical supernatural shape-shifting snake franchise returns with mystical elements.")
        },
        {
          title: t("জগদ্ধাত্রী (Zee Bangla)", "Jagaddhatri"),
          year: "2026",
          genre: t("অ্যাকশন ড্রামা", "Action Crime Drama"),
          badges: ["HD", "TOP"],
          image: "https://images.unsplash.com/photo-1542206395-9feb3edaa68d?auto=format&fit=crop&w=600&q=80", // Girl action
          desc: t("জি বাংলার রহস্য গোয়েন্দা জগদ্ধাত্রীর দুর্ধর্ষ অপরাধ দমন অ্যাকশন ও টানটান পারিবারিক রহস্য।", "A brilliant secret special-crime investigator fights criminals while managing her family life.")
        }
      ]
    },
    {
      id: "south_movies",
      title: t("🔥 সাউথ ইন্ডিয়ান ডাবড হিট মুভি", "🔥 South Indian Dubbed Hits"),
      desc: t("পুষ্পা • কেজিএফ • আরআরআর • বাহুবলী • হিন্দি ডাবড • সব ফ্রি", "Pushpa • KGF • RRR • Bahubali • Hindi Dubbed • Free"),
      items: [
        {
          title: t("কান্তারা (Kantara Dubbed)", "Kantara (Dubbed)"),
          year: "2026",
          genre: t("অ্যাকশন থ্রিলার / মিষ্টিক", "Mystic Thriller"),
          badges: ["HIT", t("ফ্রি", "FREE")],
          image: "https://images.unsplash.com/photo-1533105079780-92b9be482077?auto=format&fit=crop&w=600&q=80", // Tribal bonfire festival
          desc: t("ঐতিহ্যবাহী দেবতার বিশ্বাস ও বনের অধিকার নিয়ে তৈরি অস্কার মনোনীত অনবদ্য অ্যাকশন মিস্ট্রি।", "A spectacular story combining divine traditions, forest rights, and breathtaking action.")
        },
        {
          title: t("পুষ্পা ২: দ্য রুল (Pushpa 2)", "Pushpa 2: The Rule"),
          year: "2026",
          genre: t("অ্যাকশন ড্রামা", "Action Drama"),
          badges: ["HIT", t("ফ্রি", "FREE")],
          image: "https://images.unsplash.com/photo-1611224885990-ab7363d1f2a9?auto=format&fit=crop&w=600&q=80", // Intense flame
          desc: t("পুষ্পার রাজত্বের বিস্তার এবং পুলিশের সাথে রক্তক্ষয়ী লড়াইয়ের চোখ ধাঁধানো ট্রিলজি।", "Pushpa Raj expands his syndicate's rule in an adrenaline-pumping blockbuster sequel.")
        },
        {
          title: t("কেজিএফ ৩ টিজার (KGF 3)", "KGF: Chapter 3 Teaser"),
          year: "2026",
          genre: t("অ্যাকশন থ্রিলার", "Action Thriller"),
          badges: ["4K", t("ফ্রি", "FREE")],
          image: "https://images.unsplash.com/photo-1536440136628-849c177e76a1?auto=format&fit=crop&w=600&q=80", // Dark throne gold
          desc: t("সোনার খনিতে রকির সাম্রাজ্য নিয়ে কেজিএফ ৩-এর এক্সক্লুসিভ অফিশিয়াল টিজার ও স্ট্যাটাস।", "The exclusive first-look trailer, leaks and fan theories for Rocky Bhai's return in KGF 3.")
        },
        {
          title: t("সালার ২ (Salaar Part 2)", "Salaar: Part 2"),
          year: "2026",
          genre: t("ক্রাইম অ্যাকশন", "Crime Action"),
          badges: ["HD", t("নতুন", "NEW")],
          image: "https://images.unsplash.com/photo-1595769816263-9b910be24d5f?auto=format&fit=crop&w=600&q=80", // Heavy raw action theme
          desc: t("খানসারের রাজসিংহাসনের তুমুল দ্বন্দ এবং রক্তক্ষয়ী লড়াইয়ের দ্বিতীয় চরম অধ্যায়।", "The epic battle of ultimate friendship and destiny continues in the ruins of Khansaar.")
        }
      ]
    },
    {
      id: "kdrama_anime",
      title: t("🇰🇷 কোরিয়ান ড্রামা ও অ্যানিমে", "🇰🇷 K-Drama & Anime"),
      desc: t("স্কুইড গেম • ক্র্যাশ ল্যান্ডিং • ডেমন স্লেয়ার • ট্রু বিউটি • সব ফ্রি", "Squid Game • Crash Landing on You • Demon Slayer • True Beauty • Stream FREE"),
      items: [
        {
          title: t("স্কুইড গেম ২ (Squid Game 2)", "Squid Game Season 2"),
          year: "2026",
          genre: t("সারভাইভাল থ্রিলার", "Survival Thriller"),
          badges: ["NEW", t("ফ্রি", "FREE")],
          image: "https://images.unsplash.com/photo-1594909122845-11baa439b7bf?auto=format&fit=crop&w=600&q=80", // Surreal pink and black masked face style
          desc: t("৪৫.৬ বিলিয়ন জয়ের রক্তাক্ত খেলার দ্বিতীয় অধ্যায়ে ফিরে আসছেন গি-হুন।", "Gi-hun returns to the deadly game to stop it once and for all in this survival epic.")
        },
        {
          title: t("ডেমন স্লেয়ার (Infinity Castle)", "Demon Slayer"),
          year: "2026",
          genre: t("অ্যানিমে অ্যাকশন", "Anime Action"),
          badges: ["4K", t("ফ্রি", "FREE")],
          image: "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?auto=format&fit=crop&w=600&q=80", // Anime dark neon sword
          desc: t("ইনফিনিটি ক্যাসেল ট্রিলজির প্রথম সিনেমা - তানজিরো এবং মুজানের মহাকাব্যিক লড়াই।", "The ultimate war inside the Infinity Castle begins in this highly anticipated cinematic movie.")
        },
        {
          title: t("ক্র্যাশ ল্যান্ডিং অন ইউ", "Crash Landing on You"),
          year: "2026",
          genre: t("কোরিয়ান রোমান্স", "Korean Romance Drama"),
          badges: ["HD", t("ফ্রি", "FREE")],
          image: "https://images.unsplash.com/photo-1517059224940-d4af9eec41b7?auto=format&fit=crop&w=600&q=80", // Winter snow landscape
          desc: t("উত্তর কোরিয়ায় দুর্ঘটনাবশত অবতরণ এবং দক্ষিণ কোরিয়ার ধনকুবের সুন্দরীর প্রেম কাহিনীর অলৌকিক মেলবন্ধন।", "A paragliding accident drops a South Korean heiress in North Korea, sparking a legendary love story.")
        },
        {
          title: t("সোলো লেভেলিং সিজন ২", "Solo Leveling Season 2"),
          year: "2026",
          genre: t("অ্যানিমে ফ্যান্টাসি", "Anime Fantasy"),
          badges: ["HD", t("নতুন", "NEW")],
          image: "https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&w=600&q=80", // Shadow / Blue aura
          desc: t("শ্যাডোর রাজা সাং জিন-উ-র বিশ্ব কাঁপানো শক্তির প্রকাশ এবং মহাকাব্যিক নতুন গেট চ্যালেঞ্জ।", "The shadow monarch Sung Jin-woo unleashes his ultimate power against deadly national hunters.")
        }
      ]
    }
  ]

  // FAQs Data
  const faqs = [
    {
      q: t("১. Flimshare অ্যাপটি কি সত্যিই ফ্রি?", "1. Is Flimshare really free?"),
      a: t("হ্যাঁ, Flimshare অ্যাপটি ১০০% ফ্রি। কোনো ক্রেডিট কার্ডের প্রয়োজন নেই, কোনো গোপন চার্জ নেই এবং কোনো সাবস্ক্রিপশন ফি ছাড়াই আপনি সমস্ত মুভি, টিভি শো, নাটক এবং লাইভ স্পোর্টস দেখতে পারবেন আজীবন।", "Yes, Flimshare is absolutely free. There are no credit cards required, no hidden charges, and zero subscription fees. You can watch all movies, TV shows, and live sports forever.")
    },
    {
      q: t("২. অ্যাপটি কি আমার ফোনের জন্য নিরাপদ?", "2. Is this app safe for my Android device?"),
      a: t("সম্পূর্ণ নিরাপদ। আমাদের Flimshare APK ফাইলটি ক্ষতিকারক ভাইরাস বা ম্যালওয়্যার মুক্ত এবং গুগল প্লে প্রোটেক্ট দ্বারা সম্পূর্ণ স্ক্যান করা। আপনি কোনো দ্বিধা ছাড়াই নিশ্চিন্তে ডাউনলোড করে ইন্সটল করতে পারেন।", "Yes, absolutely. The Flimshare APK is 100% clean, digitally signed, and free of any virus or malware. It is fully compatible and safe for all Android devices.")
    },
    {
      q: t("৩. খেলা চলার সময় কি বাফারিং হবে?", "3. Will there be buffering during live matches?"),
      a: t("একদমই না। Flimshare অ্যাপটিতে ব্যবহার করা হয়েছে আল্ট্রা-স্পিড ডেডিকেটেড CDN সার্ভার। যার কারণে আপনার সাধারণ মোবাইল ইন্টারনেট (3G/4G) বা কম গতির ওয়াইফাই থাকলেও একদম বাফারিং ছাড়া সম্পূর্ণ স্মুথভাবে এইচডি স্ট্রিম উপভোগ করতে পারবেন।", "Not at all. Flimshare utilizes premium, ultra-fast CDN servers. Even on slow mobile connections or low WiFi bandwidth, you will experience buffer-free, uninterrupted HD streams.")
    },
    {
      q: t("৪. ইন্ডিয়ান সিরিয়াল ও বাংলা নাটক কি নিয়মিত আপডেট হয়?", "4. Are Indian serials & Bangla Natoks updated daily?"),
      a: t("হ্যাঁ, স্টার জলসা ও জি বাংলার সমস্ত সিরিয়াল টিভির আগেই আমাদের অ্যাপে আপলোড করা হয়। এছাড়া নতুন রিলিজ হওয়া যেকোনো বাংলা নাটক ও মুভি তাৎক্ষণিকভাবে হাই স্পিড সার্ভারে যুক্ত করা হয়।", "Yes! All episodes of Star Jalsha and Zee Bangla serials are uploaded to our app even before they air on television. New movies and Natoks are added instantly daily.")
    }
  ]

  const triggerDownload = () => {
    // Analytics track could go here
    window.location.href = APK_DOWNLOAD_URL
  }

  return (
    <div className="min-h-screen bg-brand-darkBg text-white pb-32">
      
      {/* Top Header Navbar */}
      <header className="sticky top-0 z-40 bg-brand-darkBg/95 backdrop-blur-md border-b border-brand-border px-4 py-4">
        <div className="max-w-6xl mx-auto flex items-center justify-between">
          
          {/* Logo with Green play icon and Live badge */}
          <div className="flex items-center space-x-2 cursor-pointer" onClick={() => window.scrollTo({top: 0, behavior: 'smooth'})}>
            <div className="w-10 h-10 bg-brand-green rounded-xl flex items-center justify-center shadow-lg shadow-brand-green/30 animate-pulse">
              <Play className="text-black fill-black w-5 h-5 ml-0.5" />
            </div>
            <div>
              <div className="flex items-center space-x-1.5">
                <span className="font-extrabold text-xl tracking-tight text-white font-display">FLIMSHARE</span>
                <span className="bg-brand-red text-white text-[9px] font-bold px-1.5 py-0.2 rounded-md uppercase tracking-wider animate-bounce">
                  LIVE
                </span>
              </div>
              <span className="text-[10px] block text-brand-green leading-none font-semibold uppercase tracking-wider mt-0.5">
                {t("বেস্ট ফ্রি ওটিটি পোর্টাল", "Premium Free OTT Portal")}
              </span>
            </div>
          </div>

          {/* Actions & Language Switcher */}
          <div className="flex items-center space-x-3">
            {/* Language Toggle buttons */}
            <div className="bg-[#12161F] p-1 rounded-xl border border-brand-border flex items-center">
              <button 
                onClick={() => setLang('bn')} 
                className={`text-xs font-bold px-3 py-1.5 rounded-lg transition-all ${lang === 'bn' ? 'bg-brand-green text-black' : 'text-gray-400 hover:text-white'}`}
              >
                বাং
              </button>
              <button 
                onClick={() => setLang('en')} 
                className={`text-xs font-bold px-3 py-1.5 rounded-lg transition-all ${lang === 'en' ? 'bg-brand-green text-black' : 'text-gray-400 hover:text-white'}`}
              >
                EN
              </button>
            </div>

            {/* Quick Download Header action */}
            <button 
              onClick={triggerDownload}
              className="hidden md:flex items-center space-x-1.5 bg-brand-green text-black font-extrabold text-xs px-4 py-2.5 rounded-xl hover:scale-105 active:scale-95 transition-all shadow-md shadow-brand-green/20"
            >
              <Download className="w-4 h-4" />
              <span>{t("ফ্রি ডাউনলোড", "Download Free")}</span>
            </button>
          </div>

        </div>
      </header>

      {/* Hero Header Area */}
      <section className="relative overflow-hidden px-4 pt-10 pb-16 md:pt-16 md:pb-20 border-b border-brand-border bg-gradient-to-b from-brand-green/5 via-transparent to-transparent">
        
        {/* Decorative background shapes */}
        <div className="absolute top-1/4 left-1/2 -translate-x-1/2 w-[500px] h-[500px] bg-brand-green/10 rounded-full blur-[120px] pointer-events-none" />
        
        <div className="max-w-4xl mx-auto text-center relative z-10">
          
          {/* Pulsing sub-tag */}
          <div className="inline-flex items-center space-x-2 bg-brand-green/10 border border-brand-green/30 px-4 py-1.5 rounded-full mb-6">
            <span className="w-2 h-2 rounded-full bg-brand-green animate-ping" />
            <span className="text-[11px] font-bold text-brand-green tracking-wider uppercase">
              {t("সরাসরি সম্প্রচার হচ্ছে — সম্পূর্ণ ফ্রি", "LIVE NOW — STREAMING FREE")}
            </span>
          </div>

          {/* Dynamic main catch headlines with green highlight */}
          <h1 className="text-3xl md:text-5xl font-extrabold tracking-tight text-white mb-4 leading-tight">
            {t("সরাসরি টিভি, মুভি, নাটক ও স্পোর্টস ইভেন্ট", "Movies, TV Series & Sports Events")}<br />
            <span className="text-brand-green bg-gradient-to-r from-brand-green to-brand-yellow bg-clip-text text-transparent">
              {t("দেশি-বিদেশি ১০০০+ লাইভ চ্যানেল সম্পূর্ণ ফ্রিতে", "Global TV and Sports Live Broadcasts")}
            </span><br />
            {t("কোনো সাবস্ক্রিপশন ছাড়াই লাইভ খেলা দেখুন", "Watch All Matches Live for Free")}
          </h1>

          {/* Description */}
          <p className="text-gray-400 text-sm md:text-base max-w-2xl mx-auto mb-8 leading-relaxed">
            {t(
              "১০০০+ চ্যানেল ফ্রিতে দেখুন — বাংলা টিভি, ফুটবল, ক্রিকেট, ইন্ডিয়ান সিরিয়াল ও ১৮+ এক্সক্লুসিভ কন্টেন্ট সরাসরি HD কোয়ালিটিতে। কোনো অ্যাড বা সাইন-আপের প্রয়োজন নেই। নিচের বাটনে ক্লিক করে অফিশিয়াল APK ইন্সটল করুন।",
              "Watch 1000+ channels for free — Bangla TV, football, cricket, Indian serials & exclusive 18+ content live in HD. No ads, no signup required. Install the official APK below."
            )}
          </p>

          {/* Major Glowing Download Button */}
          <div className="flex flex-col items-center justify-center space-y-3 mb-10">
            <button 
              onClick={triggerDownload}
              className="group relative flex items-center space-x-3 bg-gradient-to-r from-brand-green to-brand-yellow text-black font-extrabold text-base md:text-lg px-8 py-4.5 rounded-2xl hover:scale-[1.03] active:scale-95 transition-all shadow-xl shadow-brand-green/25"
            >
              <Download className="w-6 h-6 animate-bounce" />
              <span>{t("ফ্রি এন্ড্রয়েড অ্যাপ ডাউনলোড করুন", "Download Free APK")}</span>
              
              {/* Pulsing light borders around button */}
              <span className="absolute -inset-1 rounded-2xl bg-gradient-to-r from-brand-green to-brand-yellow opacity-30 blur-sm group-hover:opacity-40 transition-all pointer-events-none -z-10" />
            </button>
            <div className="flex items-center space-x-4 text-xs text-gray-500 font-semibold">
              <span>{t("ভার্সন: ", "Version: ")}{APP_VERSION}</span>
              <span>•</span>
              <span>{t("ফাইল সাইজ: ", "Size: ")}{APK_SIZE}</span>
              <span>•</span>
              <span>{t("প্লে প্রোটেক্ট ভেরিফাইড", "Play Protect Verified")}</span>
            </div>
          </div>

          {/* Stats Bar */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 max-w-3xl mx-auto border-t border-brand-border/60 pt-8">
            <div className="bg-brand-cardBg/60 p-4 rounded-xl border border-brand-border text-center">
              <span className="text-brand-green font-extrabold text-2xl md:text-3xl block">১০০০+</span>
              <span className="text-[11px] text-gray-400 font-bold tracking-wider block uppercase mt-1">
                {t("লাইভ চ্যানেল", "LIVE CHANNELS")}
              </span>
            </div>
            <div className="bg-brand-cardBg/60 p-4 rounded-xl border border-brand-border text-center">
              <span className="text-brand-green font-extrabold text-2xl md:text-3xl block">4K HDR</span>
              <span className="text-[11px] text-gray-400 font-bold tracking-wider block uppercase mt-1">
                {t("ভিডিও কোয়ালিটি", "4K QUALITY")}
              </span>
            </div>
            <div className="bg-brand-cardBg/60 p-4 rounded-xl border border-brand-border text-center">
              <span className="text-brand-green font-extrabold text-2xl md:text-3xl block">৫ লাখ+</span>
              <span className="text-[11px] text-gray-400 font-bold tracking-wider block uppercase mt-1">
                {t("সন্তুষ্ট ইউজার", "ACTIVE USERS")}
              </span>
            </div>
            <div className="bg-brand-cardBg/60 p-4 rounded-xl border border-brand-border text-center">
              <span className="text-brand-green font-extrabold text-2xl md:text-3xl block">১০০% ফ্রি</span>
              <span className="text-[11px] text-gray-400 font-bold tracking-wider block uppercase mt-1">
                {t("আজীবন ফ্রি", "FREE FOREVER")}
              </span>
            </div>
          </div>

        </div>
      </section>

      {/* Main Content Sections / Movie Lists */}
      <section className="max-w-6xl mx-auto px-4 py-12 space-y-16">
        {categories.map((category) => (
          <div key={category.id} className="space-y-6">
            
            {/* Category Header */}
            <div className="flex flex-col md:flex-row md:items-center justify-between border-b border-brand-border/60 pb-3 gap-2">
              <div>
                <h2 className="text-xl md:text-2xl font-extrabold text-white flex items-center">
                  {category.title}
                </h2>
                <p className="text-xs text-gray-400 mt-1">
                  {category.desc}
                </p>
              </div>
              <button 
                onClick={triggerDownload}
                className="self-start md:self-auto bg-brand-green/10 text-brand-green hover:bg-brand-green hover:text-black font-extrabold text-xs px-3 py-1.5 rounded-lg border border-brand-green/30 transition-all flex items-center space-x-1"
              >
                <span>{t("অ্যাপে দেখুন →", "In App →")}</span>
              </button>
            </div>

            {/* Horizontal Grid Slider */}
            <div className="grid grid-cols-2 md:grid-cols-4 gap-5">
              {category.items.map((item, idx) => (
                <div 
                  key={idx}
                  onClick={() => setSelectedMovie(item)}
                  className="bg-brand-cardBg border border-brand-border rounded-2xl overflow-hidden cursor-pointer hover:border-brand-green/40 hover:-translate-y-1 transition-all duration-300 flex flex-col group relative"
                >
                  
                  {/* Badges Overlay */}
                  <div className="absolute top-2.5 left-2.5 z-20 flex flex-col gap-1">
                    {item.badges.map((badge, bIdx) => (
                      <span 
                        key={bIdx}
                        className={`text-[9px] font-extrabold px-2 py-0.5 rounded-md uppercase tracking-wider ${
                          badge === 'LIVE' 
                            ? 'bg-brand-red text-white animate-pulse' 
                            : badge === 'HD' || badge === '4K'
                              ? 'bg-brand-green text-black' 
                              : 'bg-brand-yellow text-black'
                        }`}
                      >
                        {badge}
                      </span>
                    ))}
                  </div>

                  {/* Thumbnail Cover */}
                  <div className="aspect-[4/5] md:aspect-[3/4] relative overflow-hidden bg-black/50 select-none">
                    <img 
                      src={item.image} 
                      alt={item.title} 
                      className="w-full h-full object-cover group-hover:scale-105 transition-all duration-500"
                      loading="lazy"
                    />
                    <div className="absolute inset-0 bg-gradient-to-t from-brand-cardBg via-transparent to-transparent opacity-80" />
                    
                    {/* Hover play overlay */}
                    <div className="absolute inset-0 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity bg-black/40">
                      <div className="w-12 h-12 bg-brand-green rounded-full flex items-center justify-center shadow-lg shadow-brand-green/40 transform scale-75 group-hover:scale-100 transition-transform duration-300">
                        <Play className="text-black fill-black w-5 h-5 ml-0.5" />
                      </div>
                    </div>
                  </div>

                  {/* Info details */}
                  <div className="p-3.5 flex-grow flex flex-col justify-between">
                    <div>
                      <h3 className="text-xs md:text-sm font-extrabold text-gray-100 leading-snug line-clamp-1 group-hover:text-brand-green transition-all">
                        {item.title}
                      </h3>
                      <p className="text-[10px] text-gray-500 mt-0.5 font-semibold">
                        {item.year} • {item.genre}
                      </p>
                    </div>

                    <div className="mt-2 pt-2 border-t border-brand-border flex items-center justify-between">
                      <span className="text-[9px] font-bold text-gray-400 uppercase tracking-widest flex items-center gap-1">
                        <Info className="w-3.5 h-3.5 text-brand-green" />
                        {t("বিশদ বিবরণ", "DETAILS")}
                      </span>
                      <span className="text-[9px] bg-brand-green/10 text-brand-green font-extrabold px-1.5 py-0.5 rounded-md flex items-center space-x-1">
                        <span>{t("ফ্রি", "FREE")}</span>
                      </span>
                    </div>
                  </div>

                </div>
              ))}
            </div>

          </div>
        ))}

        {/* Exclusive Private 18+ Category Section (HUGE CONVERSION CONVERTOR) */}
        <div className="space-y-6">
          <div className="border-b border-brand-border/60 pb-3">
            <h2 className="text-xl md:text-2xl font-extrabold text-brand-yellow flex items-center gap-2">
              <span>🔞 {t("প্রাইভেট ১৮+ চ্যানেল (প্রাপ্তবয়স্কদের জন্য)", "Private 18+ Channels")}</span>
            </h2>
            <p className="text-xs text-gray-400 mt-1">
              {t("কেবলমাত্র প্রাপ্তবয়স্কদের জন্য • সম্পূর্ণ গোপনীয় • সরাসরি এইচডি চ্যানেল", "Adults Only • Private Viewing • HD Channels • Free in App")}
            </p>
          </div>

          {/* Under 18 Restriction Warning Box exactly as screenshot */}
          <div className="bg-gradient-to-r from-red-950/20 via-[#1A1012] to-red-950/20 border border-brand-red/30 rounded-3xl p-8 max-w-2xl mx-auto text-center relative overflow-hidden shadow-xl shadow-brand-red/5">
            <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-brand-red to-orange-500" />
            
            <div className="w-16 h-16 bg-brand-red/10 rounded-full flex items-center justify-center mx-auto mb-4 border border-brand-red/30">
              <span className="text-brand-red text-2xl font-extrabold">18+</span>
            </div>

            <h3 className="text-xl font-black text-white mb-2">
              {t("প্রাইভেট ১৮+ কন্টেন্ট লকার", "Private 18+ Content")}
            </h3>
            
            <p className="text-gray-400 text-xs md:text-sm max-w-md mx-auto mb-6 leading-relaxed">
              {t(
                "প্রাপ্তবয়স্কদের ১৮+ কন্টেন্ট ও হাই-কোয়ালিটি হট লাইভ ভিডিও চ্যানেলগুলো সিকিউরভাবে অ্যাপের ভেতরে উপলব্ধ। নিরাপদে এবং গোপনে দেখতে অফিশিয়াল Flimshare অ্যাপটি ডাউনলোড করুন। কোনো ক্রেডিট কার্ড বা সাইন-আপ লাগবে না।",
                "Private 18+ video posts & hot channels are securely integrated inside the application. Secure and discreet. Install the Flimshare app to unlock unrestricted private access instantly."
              )}
            </p>

            <button 
              onClick={triggerDownload}
              className="bg-gradient-to-r from-brand-red to-orange-600 text-white font-extrabold text-sm md:text-base px-6 py-3.5 rounded-2xl hover:scale-105 active:scale-95 transition-all shadow-lg shadow-brand-red/30 inline-flex items-center space-x-2"
            >
              <Download className="w-5 h-5" />
              <span>{t("অ্যাপে দেখুন — সম্পূর্ণ ফ্রি", "Open in App — Free Access")}</span>
            </button>
            
            <div className="mt-4 text-[10px] text-gray-500 font-bold flex items-center justify-center gap-1.5">
              <span>{t("গোপনীয়তা নিশ্চিত", "100% Secure & Private")}</span>
              <span>•</span>
              <span>{t("কোনো লগ বা ট্র্যাকিং নেই", "No History / Logs Saved")}</span>
            </div>
          </div>
        </div>

      </section>

      {/* Feature Showcase Grid section */}
      <section className="bg-gradient-to-b from-[#12161F] to-transparent py-16 px-4 border-y border-brand-border">
        <div className="max-w-5xl mx-auto">
          
          <div className="text-center mb-12">
            <h2 className="text-2xl md:text-3xl font-black text-white mb-3">
              {t("কেন আপনি Flimshare ওটিটি ব্যবহার করবেন?", "Why Choose Flimshare OTT?")}
            </h2>
            <p className="text-gray-400 text-xs md:text-sm max-w-lg mx-auto">
              {t(
                "অন্যান্য ওটিটি প্ল্যাটফর্মের তুলনায় Flimshare-এ রয়েছে চমৎকার সব হাই-টেক ফিচার যা আপনাকে দেবে সেরা স্ট্রিমিং অভিজ্ঞতা।",
                "Unlike ordinary stream portals, Flimshare comes with high-fidelity streaming engines to double your visual comfort."
              )}
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            
            <div className="bg-brand-darkBg border border-brand-border p-6 rounded-2xl">
              <div className="w-12 h-12 bg-brand-green/10 rounded-xl flex items-center justify-center text-brand-green mb-4">
                <Wifi className="w-6 h-6" />
              </div>
              <h3 className="text-base font-bold text-white mb-2">
                {t("⚡ আল্ট্রা-স্পিড CDN সার্ভার", "⚡ Super-Fast Streaming")}
              </h3>
              <p className="text-xs text-gray-400 leading-relaxed">
                {t(
                  "প্রতিটি লাইভ চ্যানেল ও ভিডিওর জন্য রয়েছে বিশ্বমানের আল্ট্রা-ফাস্ট ডেডিকেটেড ক্যাশ লেয়ার সার্ভার। বাফারিং বা লেগ ছাড়াই চলবে খেলা ও মুভি।",
                  "Every match and channel is backed by top-tier cloud nodes and global caching systems, delivering flawless streams even on 3G."
                )}
              </p>
            </div>

            <div className="bg-brand-darkBg border border-brand-border p-6 rounded-2xl">
              <div className="w-12 h-12 bg-brand-green/10 rounded-xl flex items-center justify-center text-brand-green mb-4">
                <Shield className="w-6 h-6" />
              </div>
              <h3 className="text-base font-bold text-white mb-2">
                {t("🚫 বিরক্তিকর বিজ্ঞাপন মুক্ত", "🚫 Ad-Free Experience")}
              </h3>
              <p className="text-xs text-gray-400 leading-relaxed">
                {t(
                  "স্ট্রিমিং বা খেলা চলাকালীন মাঝপথে বিরক্তিকর ভিডিও বিজ্ঞাপন সম্পূর্ণ বন্ধ। কোনো ডিস্টার্ব ছাড়াই উপভোগ করুন আপনার প্রিয় শো।",
                  "Say goodbye to annoying popups and mid-roll commercials. Get clean, direct content with absolutely zero interruptions."
                )}
              </p>
            </div>

            <div className="bg-brand-darkBg border border-brand-border p-6 rounded-2xl">
              <div className="w-12 h-12 bg-brand-green/10 rounded-xl flex items-center justify-center text-brand-green mb-4">
                <Tv className="w-6 h-6" />
              </div>
              <h3 className="text-base font-bold text-white mb-2">
                {t("📺 স্মার্ট টিভি ও ক্রোমকাস্ট", "📺 Smart TV & Chromecast")}
              </h3>
              <p className="text-xs text-gray-400 leading-relaxed">
                {t(
                  "আপনার মোবাইলের স্ক্রিন এক ক্লিকে সরাসরি স্মার্ট টিভি বা ক্রোমকাস্ট ডিভাইসে বড় পর্দায় কাস্ট করুন এবং পুরো পরিবারের সাথে আনন্দ নিন।",
                  "Cast your smartphone stream to any smart television, Firestick, or Chromecast with one tap to enjoy movies with your family."
                )}
              </p>
            </div>

          </div>

        </div>
      </section>

      {/* Installation Step Guide */}
      <section className="max-w-4xl mx-auto px-4 py-16">
        <div className="text-center mb-10">
          <h2 className="text-xl md:text-2xl font-black text-white">
            {t("কিভাবে Flimshare অ্যাপ ইন্সটল করবেন?", "How to Install Flimshare App?")}
          </h2>
          <p className="text-xs text-gray-400 mt-2">
            {t("নিচের ৩টি সহজ ধাপ অনুসরণ করে আজীবনের জন্য ফ্রি স্ট্রিমিং শুরু করুন", "Follow these 3 simple steps to start streaming for free forever")}
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 text-center">
          <div className="bg-brand-cardBg/30 border border-brand-border/80 p-5 rounded-2xl relative">
            <div className="w-8 h-8 bg-brand-green text-black rounded-full flex items-center justify-center font-bold text-sm mx-auto mb-3">1</div>
            <h4 className="text-sm font-bold text-white mb-1.5">{t("APK ডাউনলোড করুন", "Download APK")}</h4>
            <p className="text-[11px] text-gray-400">{t("বাটনে ক্লিক করে Flimshare v1.0.apk ফাইলটি ডাউনলোড করুন।", "Click any download button to save the official APK file.")}</p>
          </div>
          <div className="bg-brand-cardBg/30 border border-brand-border/80 p-5 rounded-2xl relative">
            <div className="w-8 h-8 bg-brand-green text-black rounded-full flex items-center justify-center font-bold text-sm mx-auto mb-3">2</div>
            <h4 className="text-sm font-bold text-white mb-1.5">{t("Unknown Sources অন করুন", "Allow Unknown Sources")}</h4>
            <p className="text-[11px] text-gray-400">{t("সেটিংস থেকে 'Install from Unknown Sources' অথবা গুগল ক্রোম পারমিশন অন করুন।", "Enable unknown sources installation under Android security settings.")}</p>
          </div>
          <div className="bg-brand-cardBg/30 border border-brand-border/80 p-5 rounded-2xl relative">
            <div className="w-8 h-8 bg-brand-green text-black rounded-full flex items-center justify-center font-bold text-sm mx-auto mb-3">3</div>
            <h4 className="text-sm font-bold text-white mb-1.5">{t("ইন্সটল করে উপভোগ করুন", "Install & Stream")}</h4>
            <p className="text-[11px] text-gray-400">{t("ফাইলটি ওপেন করে ইন্সটল করুন এবং আনলিমিটেড লাইভ স্পোর্টস ফ্রিতে উপভোগ করুন।", "Open the downloaded file, click install, and enjoy infinite free streams.")}</p>
          </div>
        </div>
      </section>

      {/* Accordion FAQ section */}
      <section className="max-w-3xl mx-auto px-4 py-8 border-t border-brand-border/60">
        <div className="text-center mb-8">
          <h2 className="text-xl md:text-2xl font-black text-white">
            {t("সচরাচর জিজ্ঞাসিত প্রশ্নাবলী (FAQs)", "Frequently Asked Questions")}
          </h2>
        </div>

        <div className="space-y-3.5">
          {faqs.map((faq, idx) => {
            const isOpen = activeFaq === idx
            return (
              <div 
                key={idx}
                className="bg-[#12161F] border border-brand-border rounded-xl overflow-hidden transition-all duration-300"
              >
                <button 
                  onClick={() => setActiveFaq(isOpen ? null : idx)}
                  className="w-full p-4 flex items-center justify-between text-left focus:outline-none"
                >
                  <span className="text-xs md:text-sm font-bold text-white">
                    {faq.q}
                  </span>
                  {isOpen ? (
                    <ChevronUp className="w-4 h-4 text-brand-green shrink-0 ml-2" />
                  ) : (
                    <ChevronDown className="w-4 h-4 text-brand-green shrink-0 ml-2" />
                  )}
                </button>
                
                {isOpen && (
                  <div className="p-4 pt-0 border-t border-brand-border text-xs md:text-sm text-gray-400 leading-relaxed bg-[#0E1118]">
                    {faq.a}
                  </div>
                )}
              </div>
            )
          })}
        </div>
      </section>

      {/* Footer disclaimer and rights */}
      <footer className="border-t border-brand-border/60 mt-16 py-12 px-4 text-center bg-[#090B0E]">
        <div className="max-w-6xl mx-auto space-y-4">
          <div className="flex justify-center items-center space-x-2">
            <div className="w-8 h-8 bg-brand-green rounded-lg flex items-center justify-center">
              <Play className="text-black fill-black w-4 h-4 ml-0.5" />
            </div>
            <span className="font-extrabold text-base tracking-tight text-white">FLIMSHARE OTT</span>
          </div>
          
          <p className="text-[11px] text-gray-500 max-w-xl mx-auto leading-relaxed">
            {t(
              "দাবিত্যাগ: Flimshare একটি স্বাধীন মোবাইল অ্যাপ্লিকেশন যা ব্যবহারকারীদের বিভিন্ন ওয়েবে উপলব্ধ ফ্রি মিডিয়া লিংক সহজে স্ট্রিমিং করতে সাহায্য করে। আমরা কোনো অননুমোদিত কন্টেন্ট হোস্ট করি না। কপিরাইট বিষয়ক সমস্ত তথ্য DMCA নোটিশ অনুসারে নিষ্পত্তি করা হয়।",
              "Disclaimer: Flimshare is a search and streaming client application providing clean user guides. We do not host or own unauthorized streams. All media assets are gathered from open public servers. DMCA rights respected."
            )}
          </p>

          <div className="flex flex-wrap justify-center gap-4 text-xs font-semibold text-gray-400 pt-2">
            <a href="#dmca" className="hover:text-brand-green transition-all">DMCA Notice</a>
            <span>•</span>
            <a href="#privacy" className="hover:text-brand-green transition-all">Privacy Policy</a>
            <span>•</span>
            <a href="#terms" className="hover:text-brand-green transition-all">Terms of Use</a>
            <span>•</span>
            <a href="#contact" className="hover:text-brand-green transition-all">Contact Support</a>
          </div>

          <p className="text-[10px] text-gray-600 pt-4">
            &copy; 2026 Flimshare OTT. Developed with ❤️ for Android Streaming Community.
          </p>
        </div>
      </footer>

      {/* Floating Bottom Download Bar (STICKY & PERSISTENT on Mobile / Wide screen) */}
      <div className="fixed bottom-0 left-0 right-0 z-50 bg-[#0E1118]/95 backdrop-blur-md border-t border-brand-green/30 px-3 py-3 md:py-4 flex justify-center items-center">
        <div className="max-w-3xl w-full flex flex-col sm:flex-row items-center justify-between gap-3 px-2">
          
          <div className="hidden sm:flex items-center space-x-3 text-left">
            <div className="w-10 h-10 bg-brand-green rounded-xl flex items-center justify-center shrink-0">
              <Download className="text-black w-5 h-5" />
            </div>
            <div>
              <h4 className="text-xs md:text-sm font-extrabold text-white leading-tight">
                Flimshare — {t("১০০% ফ্রি ওটিটি অ্যাপ", "100% Free Live Streaming")}
              </h4>
              <p className="text-[9px] md:text-[10px] text-gray-400 font-bold mt-0.5">
                Android • {APK_SIZE} • {t("কোনো সাইন-আপ বা বিজ্ঞাপন নেই", "No Signup • No Subscription")}
              </p>
            </div>
          </div>

          {/* Glowing Green Download Button on Bottom Stick */}
          <button 
            onClick={triggerDownload}
            className="w-full sm:w-auto bg-gradient-to-r from-brand-green to-brand-yellow text-black font-extrabold text-xs md:text-sm px-6 py-3.5 rounded-xl hover:scale-105 active:scale-95 transition-all shadow-md shadow-brand-green/35 flex items-center justify-center space-x-2.5"
          >
            <Download className="w-4 h-4 animate-bounce" />
            <span>{t("ডাউনলোড Flimshare — ১০০% ফ্রি", "Download Flimshare — 100% Free")}</span>
          </button>
          
          <span className="sm:hidden text-[9px] text-gray-400 font-extrabold text-center block -mt-1.5">
            Android • {APK_SIZE} • {t("কোনো সাইন-আপ নেই • নো অ্যাডস", "No Signup • No Subscription")}
          </span>

        </div>
      </div>

      {/* Enticing Detail / In-App Play Modal Popup */}
      {selectedMovie && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-brand-cardBg border border-brand-border w-full max-w-md rounded-2xl overflow-hidden shadow-2xl relative">
            
            {/* Header / Cover image inside modal */}
            <div className="aspect-video relative overflow-hidden bg-black">
              <img src={selectedMovie.image} alt={selectedMovie.title} className="w-full h-full object-cover" />
              <div className="absolute inset-0 bg-gradient-to-t from-brand-cardBg to-transparent" />
              
              {/* Overlay content badges */}
              <div className="absolute top-3 left-3 flex gap-1">
                {selectedMovie.badges.map((badge, idx) => (
                  <span key={idx} className="text-[9px] bg-brand-green text-black font-extrabold px-1.5 py-0.5 rounded uppercase tracking-wider">{badge}</span>
                ))}
              </div>

              {/* Close Button */}
              <button 
                onClick={() => setSelectedMovie(null)}
                className="absolute top-3 right-3 w-8 h-8 bg-black/60 hover:bg-black text-white rounded-full flex items-center justify-center transition-all focus:outline-none"
              >
                &times;
              </button>
            </div>

            {/* Modal Body Info details */}
            <div className="p-5 space-y-4">
              <div>
                <h3 className="text-lg font-black text-white">{selectedMovie.title}</h3>
                <p className="text-xs text-brand-green font-bold mt-1">
                  {selectedMovie.year} • {selectedMovie.genre}
                </p>
              </div>

              <p className="text-xs text-gray-400 leading-relaxed bg-brand-darkBg/50 p-3 rounded-lg border border-brand-border">
                {selectedMovie.desc}
              </p>

              {/* Install and Download prompt */}
              <div className="bg-brand-green/5 border border-brand-green/20 p-4 rounded-xl text-center space-y-3">
                <p className="text-xs font-bold text-gray-200">
                  {t(
                    "এই ভিডিওটি এবং সরাসরি ১০০০+ লাইভ টিভি চ্যানেল সম্পূর্ণ ফ্রিতে বাফারিং ছাড়া দেখতে আমাদের অফিশিয়াল অ্যাপটি ইন্সটল করুন।",
                    "To watch this video and stream 1000+ live television channels for free without buffering, install our official app."
                  )}
                </p>

                <button 
                  onClick={() => {
                    triggerDownload()
                    setSelectedMovie(null)
                  }}
                  className="w-full bg-brand-green hover:bg-brand-green/90 text-black font-extrabold text-sm py-3 rounded-xl transition-all shadow-md shadow-brand-green/20 flex items-center justify-center space-x-2"
                >
                  <Download className="w-4 h-4" />
                  <span>{t("অ্যাপ ইন্সটল করুন (১৮ এমবি)", "Install Free App (18 MB)")}</span>
                </button>
              </div>

              <div className="flex justify-between items-center text-[10px] text-gray-500 font-semibold px-1">
                <span>OS: Android 5.0+</span>
                <span>•</span>
                <span>Type: Official APK</span>
                <span>•</span>
                <span>License: 100% Free</span>
              </div>

            </div>

          </div>
        </div>
      )}

    </div>
  )
}
