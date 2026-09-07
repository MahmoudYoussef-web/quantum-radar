export function Loading({ what }: { what: string }) {
  return (
    <p className="muted" role="status">
      Loading {what}…
    </p>
  )
}

export function ErrorBox({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <div className="error-box" role="alert">
      <p style={{ margin: '0 0 0.5rem' }}>Failed to load: {message}</p>
      <button className="btn-ghost btn btn-sm" onClick={onRetry}>
        Retry
      </button>
    </div>
  )
}

export function Empty({ what }: { what: string }) {
  return <p className="muted">No {what} found.</p>
}

export function Pager({
  page,
  totalPages,
  onPage,
}: {
  page: number
  totalPages: number
  onPage: (p: number) => void
}) {
  if (totalPages <= 1) return null
  return (
    <nav className="pager" aria-label="Pagination">
      <button className="btn-ghost btn btn-sm" disabled={page === 0} onClick={() => onPage(page - 1)}>
        Prev
      </button>
      <span aria-live="polite">
        Page {page + 1} of {totalPages}
      </span>
      <button
        className="btn-ghost btn btn-sm"
        disabled={page + 1 >= totalPages}
        onClick={() => onPage(page + 1)}
      >
        Next
      </button>
    </nav>
  )
}
