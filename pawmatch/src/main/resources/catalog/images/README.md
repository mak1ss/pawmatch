# Breed images

Bundled breed photos, served by `GET /api/v1/breeds/{slug}/image`.

To add a photo:

1. Put the file here as `<slug>.webp` (or `.jpg` / `.png`). Keep it small, about 800 px wide.
2. Add a Liquibase changeset inserting a `breed_image` row: `breed_id` (looked up by slug), `storage_key` (the file name),
   `content_type`, and the attribution columns `source_url`, `author` and `license`.

Only use photos we're allowed to show (e.g. Wikimedia Commons CC BY / CC BY-SA with attribution, or team-owned).
AKC page photos are copyrighted and must not be used.
