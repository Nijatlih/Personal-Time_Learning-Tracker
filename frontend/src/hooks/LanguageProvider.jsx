import { useCallback, useLayoutEffect, useMemo, useState } from 'react';
import { LanguageContext } from './useLanguage';
import { LANG_STORAGE_KEY, getStoredLang, translate } from '../i18n';

export const LanguageProvider = ({ children }) => {
  const [lang, setLang] = useState(getStoredLang);

  useLayoutEffect(() => {
    document.documentElement.lang = lang === 'zh' ? 'zh-CN' : 'en';
  }, [lang]);

  const t = useCallback((key, params) => translate(lang, key, params), [lang]);

  const value = useMemo(() => ({
    lang,
    t,
    toggleLang: () => {
      setLang((current) => {
        const next = current === 'en' ? 'zh' : 'en';
        try {
          localStorage.setItem(LANG_STORAGE_KEY, next);
        } catch {
          // 存不进去也不影响本次会话内的切换
        }
        return next;
      });
    },
  }), [lang, t]);

  return <LanguageContext.Provider value={value}>{children}</LanguageContext.Provider>;
};

export default LanguageProvider;
