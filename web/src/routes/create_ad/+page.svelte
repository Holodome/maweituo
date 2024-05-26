<script lang="ts">
  import { enhance } from '$app/forms';
</script>

<div class="card shrink-0 w-full max-w-sm shadow-2xl bg-base-100 mx-auto">
  <form class="card-body" use:enhance method="POST">
    <h3>New advertisement</h3>
    <div class="form-control">
      <label class="label" for="name">
        <span class="label-text">Title</span>
      </label>
      <input
        id="title"
        name="title"
        type="text"
        placeholder="title"
        class="input input-bordered"
        required
      />
    </div>
    <div class="form-control mt-6">
      <button class="btn btn-primary" type="submit">Create</button>
    </div>
  </form>
</div>
