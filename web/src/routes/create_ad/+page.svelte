<script lang="ts">
  import { enhance } from '$app/forms';
</script>

<div>
  <form use:enhance method="POST">
    <fieldset>
      <input name="title" type="text" required placeholder="Title" />
    </fieldset>
    <button type="submit">Create ad</button>
  </form>
</div>
