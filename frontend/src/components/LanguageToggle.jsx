import { useLanguage } from '../hooks/useLanguage';

const LanguageToggle = () => {
  const { lang, toggleLang, t } = useLanguage();

  return (
    <button
      type="button"
      className="icon-button theme-toggle language-toggle"
      onClick={toggleLang}
      aria-label={t('lang.switch')}
      title={t('lang.title')}
    >
      {lang === 'en' ? '中' : 'EN'}
    </button>
  );
};

export default LanguageToggle;
