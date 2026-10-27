import en from './en';
import zh from './zh';

export const LANG_STORAGE_KEY = 'lang';
export const SUPPORTED_LANGS = ['en', 'zh'];
const DICTIONARIES = { en, zh };

// 读取上次选择的语言；没有或读取失败就用英文。api/client.js 也用它，因为拦截器在 React 之外。
export const getStoredLang = () => {
  try {
    const stored = localStorage.getItem(LANG_STORAGE_KEY);
    if (SUPPORTED_LANGS.includes(stored)) return stored;
  } catch {
    // localStorage 不可用（隐私模式等）时退回默认语言
  }
  return 'en';
};

// 查词典并把 {name} 形式的占位符替换成 params 里的值。
// 缺键时先退回英文，再退回键名本身，保证页面不会渲染出 undefined。
export const translate = (lang, key, params) => {
  const template = DICTIONARIES[lang]?.[key] ?? en[key] ?? key;
  if (!params) return template;
  return template.replace(/\{(\w+)\}/g, (match, name) => (name in params ? String(params[name]) : match));
};
