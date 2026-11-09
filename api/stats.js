module.exports = async function handler(req, res) {
  res.setHeader('Cache-Control', 's-maxage=60, stale-while-revalidate=300');
  res.setHeader('Content-Type', 'application/json; charset=utf-8');
  try {
    const response = await fetch('https://api.github.com/repos/fagiteemmanuel4-bit/mergesignal/releases?per_page=100', {
      headers: { Accept: 'application/vnd.github+json', 'X-GitHub-Api-Version': '2022-11-28' }
    });
    if (!response.ok) throw new Error('GitHub release lookup failed: ' + response.status);
    const releases = await response.json();
    const latest = releases.find(r => !r.draft && !r.prerelease) || releases[0];
    const apk = (latest?.assets || []).find(a => a.name === 'PromptNotes.apk');
    const totalDownloads = releases.reduce((sum, release) =>
      sum + (release.assets || []).filter(a => a.name === 'PromptNotes.apk').reduce((n, asset) => n + (Number(asset.download_count) || 0), 0), 0);
    res.status(200).json({
      totalDownloads,
      latestBuild: latest?.tag_name || 'Latest',
      releaseName: latest?.name || 'Prompt Notes',
      downloadUrl: apk?.browser_download_url || 'https://github.com/fagiteemmanuel4-bit/mergesignal/releases/latest/download/PromptNotes.apk',
      sizeMb: apk?.size ? apk.size / 1048576 : null,
      publishedAt: latest?.published_at || null
    });
  } catch (error) {
    res.status(502).json({ error: 'Release statistics are temporarily unavailable.' });
  }
};
